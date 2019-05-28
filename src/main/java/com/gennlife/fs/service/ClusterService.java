package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.controlflow.function.ThrowableConsumer;
import com.gennlife.darren.controlflow.function.ThrowableRunnable;
import com.gennlife.darren.util.Endpoint;
import com.gennlife.darren.util.ImmutableEndpoint;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.common.exception.ResponseCode;
import com.gennlife.fs.common.utils.HttpRequestUtil;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.val;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gennlife.darren.controlflow.exception.Suppress.suppress;
import static com.gennlife.fs.common.utils.KeyPathUtil.toKeyPath;
import static com.gennlife.fs.common.utils.KeyPathUtil.toRedisKey;
import static com.gennlife.fs.controller.ClusterController.*;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

@Service
public class ClusterService implements ServletContextListener, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    static class ClusterInfo {

        ClusterInfo() {}

        ImmutableEndpoint master = null;
        Set<ImmutableEndpoint> slaves = new HashSet<>();

        boolean isValid() {
            return master != null;
        }

        boolean isActive(ImmutableEndpoint endpoint) {
            return Objects.equals(endpoint, master) || slaves.contains(endpoint);
        }

        static ClusterInfo fromJSON(JSON o) {
            val ret = new ClusterInfo();
            ret.master = new ImmutableEndpoint(MASTER_PATH.resolveAsString(o));
            ret.slaves = SLAVES_PATH
                .resolveAsJSONArray(o)
                .stream()
                .map(String.class::cast)
                .map(ImmutableEndpoint::new)
                .collect(toSet());
            return ret;
        }

        JSONObject toJSON() {
            val o = new JSONObject();
            MASTER_PATH.assign(o, master.toString());
            SLAVES_PATH.assign(o, slaves
                .stream()
                .map(Endpoint::toString)
                .collect(toCollection(JSONArray::new)));
            return o;
        }

        private static final KeyPath MASTER_PATH = toKeyPath("master");
        private static final KeyPath SLAVES_PATH = toKeyPath("slaves");

    }

    public JSONObject info() {
        synchronized (clusterInfoMutex) {
            return clusterInfo.toJSON();
        }
    }

    @Builder
    public static class RegisterParameters {
        public ImmutableEndpoint node;
    }

    public JSONObject registerSlave(RegisterParameters params) {
        val slave = params.node;
        synchronized (clusterInfoMutex) {
            if (!clusterInfo.slaves.contains(slave)) {
                clusterInfo.slaves.add(slave);
                broadcastClusterEvent(o -> o.clusterNodesConnected(ImmutableSet.of(slave)));
            }
        }
        val timer = new Timer();
        Optional
            .ofNullable(slaveFailureProcessors.put(slave, timer))
            .ifPresent(t -> suppress(t::cancel));
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (clusterInfoMutex) {
                    clusterInfo.slaves.remove(slave);
                }
                slaveFailureProcessors.remove(slave);
                broadcastClusterEvent(o -> o.clusterNodesDisconnected(ImmutableSet.of(slave)));
            }
        }, cfg.clusterHeartbeatInterval * cfg.clusterRetryLimit);
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    public void addClusterEventListener(ClusterEventListener listener) {
        clusterEventListeners.add(listener);
    }

    public void removeClusterEventListener(ClusterEventListener listener) {
        clusterEventListeners.remove(listener);
    }

    public boolean isActive(ImmutableEndpoint endpoint) {
        synchronized (clusterInfoMutex) {
            return clusterInfo.isActive(endpoint);
        }
    }

    private void mainLoop() {
        while (!shouldExit.get()) {
            try {
                val local = cfg.localEndpoint;
                val lock = redisson.getLock(lockKey);
                if (lock.tryLock(0, cfg.clusterRetryLimit * cfg.clusterHeartbeatInterval, MILLISECONDS)) {
                    LOGGER.debug("Lock succeeded, local: " + cfg.localEndpoint);
                    redisson.<ImmutableEndpoint>getBucket(masterKey).set(local);
                    slaveFailureProcessors.clear();
                    synchronized (clusterInfoMutex) {
                        val oldMaster = clusterInfo.master;
                        clusterInfo.slaves.remove(local);
                        clusterInfo.master = local;
                        if (!local.equals(oldMaster)) {
                            broadcastClusterEvent(o -> o.clusterMasterChanged(oldMaster, local));
                            if (oldMaster != null) {
                                broadcastClusterEvent(o -> o.clusterNodesDisconnected(ImmutableSet.of(oldMaster)));
                            }
                        }
                    }
                    int retry = 0;
                    while (!shouldExit.get()) {
                        try {
                            if (!lock.tryLock(0, cfg.clusterRetryLimit * cfg.clusterHeartbeatInterval, MILLISECONDS)) {
                                throw new Exception();
                            }
                            retry = 0;
                        } catch (Exception e) {
                            if (++retry > cfg.clusterRetryLimit) {
                                break;
                            }
                        }
                        suppress(() -> sleep(cfg.clusterHeartbeatInterval));
                    }
                } else {
                    LOGGER.debug("Lock failed, local: " + cfg.localEndpoint);
                    int retry = 0;
                    while (!shouldExit.get()) {
                        try {
                            val master = redisson.<ImmutableEndpoint>getBucket(masterKey).get();
                            if (master.equals(local)) {
                                throw new Exception();
                            }
                            synchronized (clusterInfoMutex) {
                                val oldMaster = clusterInfo.master;
                                if (!master.equals(oldMaster)) {
                                    broadcastClusterEvent(o -> o.clusterMasterChanged(oldMaster, master));
                                    if (oldMaster != null) {
                                        broadcastClusterEvent(o -> o.clusterNodesDisconnected(ImmutableSet.of(oldMaster)));
                                    }
                                    clusterInfo.master = master;
                                    if (clusterInfo.slaves.contains(master)) {
                                        clusterInfo.slaves.remove(master);
                                    } else {
                                        broadcastClusterEvent(o -> o.clusterNodesConnected(ImmutableSet.of(master)));
                                    }
                                }
                            }
                            HttpRequestUtil.postData(master,
                                cfg.applicationContextPath + CLUSTER_API_PATH + REGISTER_SLAVE_API_SUB_PATH,
                                new JSONObject()
                                    .fluentPut("node", local.toString()));
                            val response = HttpRequestUtil.get(master,
                                cfg.applicationContextPath + CLUSTER_API_PATH + INFO_API_SUB_PATH);
                            val info = ClusterInfo.fromJSON(JSON.parseObject(response));
                            synchronized (clusterInfoMutex) {
                                val downs = new HashSet<>(clusterInfo.slaves);
                                downs.removeAll(info.slaves);
                                downs.remove(local);
                                val ups = new HashSet<>(info.slaves);
                                ups.removeAll(clusterInfo.slaves);
                                ups.remove(local);
                                info.slaves.add(local);
                                clusterInfo.slaves = info.slaves;
                                if (!downs.isEmpty()) {
                                    broadcastClusterEvent(o -> o.clusterNodesDisconnected(ImmutableSet.copyOf(downs)));
                                }
                                if (!ups.isEmpty()) {
                                    broadcastClusterEvent(o -> o.clusterNodesConnected(ImmutableSet.copyOf(ups)));
                                }
                            }
                            retry = 0;
                        } catch (Exception e) {
                            if (++retry > cfg.clusterRetryLimit) {
                                break;
                            }
                        }
                        suppress(() -> sleep(cfg.clusterHeartbeatInterval));
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            suppress(() -> sleep(cfg.clusterHeartbeatInterval));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        lockKey = K("lock");
        masterKey = K("master");
        redisson = Redisson.create(new Config() {{
            useClusterServers()
                .addNodeAddress(cfg.redisServerEndpoints
                    .stream()
                    .map(Endpoint::toString)
                    .map("redis://"::concat)
                    .toArray(String[]::new))
                .setKeepAlive(true);
        }});
        addClusterEventListener(clusterEventLogger);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        mainLoopThread.start();
        LOGGER.info(getClass().getSimpleName() + " started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        shouldExit.set(true);
        suppress((ThrowableRunnable<InterruptedException>)mainLoopThread::join);
        eventBroadcaster.shutdownNow();
        slaveFailureProcessors.values().forEach(Timer::cancel);
        redisson.shutdown();
        LOGGER.info(getClass().getSimpleName() + " stopped.");
    }

    private void broadcastClusterEvent(ThrowableConsumer<ClusterEventListener, Exception> f) {
        eventBroadcaster.execute(() -> {
            for (val listener: clusterEventListeners) {
                try {
                    f.accept(listener);
                } catch (Throwable e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }
        });
    }

    private static final String CLUSTER_NAMESPACE = "cluster";

    private final Object clusterInfoMutex = new Object();

    private final ConcurrentMap<ImmutableEndpoint, Timer> slaveFailureProcessors = new ConcurrentHashMap<>();
    private final Set<ClusterEventListener> clusterEventListeners = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor eventBroadcaster = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);

    @Autowired
    private GeneralConfiguration cfg;

    @Autowired
    private ClusterEventLogger clusterEventLogger;

    private RedissonClient redisson = null;
    private final Thread mainLoopThread = new Thread(this::mainLoop);
    private final AtomicBoolean shouldExit = new AtomicBoolean(false);
    private final ClusterInfo clusterInfo = new ClusterInfo();

    private String lockKey;
    private String masterKey;

    private String K(String ...keys) {
        return toRedisKey(new KeyPath(cfg.redisFsNamespace, CLUSTER_NAMESPACE, new KeyPath(keys)));
    }

}

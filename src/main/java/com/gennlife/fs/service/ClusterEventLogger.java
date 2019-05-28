package com.gennlife.fs.service;

import com.gennlife.darren.util.ImmutableEndpoint;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ClusterEventLogger implements ClusterEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterEventLogger.class);

    @Override
    public void clusterMasterChanged(ImmutableEndpoint from, ImmutableEndpoint to) {
        LOGGER.warn("[CLUSTER] Master changed from " + from + " to " + to);
    }

    @Override
    public void clusterNodesConnected(ImmutableSet<ImmutableEndpoint> nodes) {
        LOGGER.warn("[CLUSTER] Connected to node(s) " + nodes);
    }

    @Override
    public void clusterNodesDisconnected(ImmutableSet<ImmutableEndpoint> nodes) {
        LOGGER.warn("[CLUSTER] Disconnected from node(s) " + nodes);
    }

    @Autowired
    private GeneralConfiguration cfg;

}

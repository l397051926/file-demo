package com.gennlife.fs.controller;

import com.gennlife.darren.util.ImmutableEndpoint;
import com.gennlife.fs.service.ClusterService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping(ClusterController.CLUSTER_API_PATH)
public class ClusterController extends ControllerBase {

    @RequestMapping(value = INFO_API_SUB_PATH, method = GET, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String info() {
        return run(clusterService::info);
    }

    @RequestMapping(value = REGISTER_SLAVE_API_SUB_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String registerSlave(@RequestBody String request) {
        return run(request, o -> {
            val params = ClusterService.RegisterParameters.builder()
                .node(new ImmutableEndpoint(o.getString("node")))
                .build();
            return clusterService.registerSlave(params);
        });
    }

    public static final String CLUSTER_API_PATH = "/Cluster";
    public static final String INFO_API_SUB_PATH = "/Info";
    public static final String REGISTER_SLAVE_API_SUB_PATH = "/RegisterSlave";

    @Autowired
    private ClusterService clusterService;

}

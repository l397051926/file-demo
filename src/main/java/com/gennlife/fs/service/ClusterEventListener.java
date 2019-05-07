package com.gennlife.fs.service;

import com.gennlife.darren.util.ImmutableEndpoint;
import com.google.common.collect.ImmutableSet;

public interface ClusterEventListener {

    default void clusterMasterChanged(ImmutableEndpoint from, ImmutableEndpoint to) throws Exception {}
    default void clusterNodesConnected(ImmutableSet<ImmutableEndpoint> nodes) throws Exception {}
    default void clusterNodesDisconnected(ImmutableSet<ImmutableEndpoint> nodes) throws Exception {}

}

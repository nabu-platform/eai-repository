package be.nabu.eai.repository.api;

import be.nabu.libs.cluster.api.ClusterInstance;

public interface ClusteredServer {
	public ClusterInstance getCluster();
}

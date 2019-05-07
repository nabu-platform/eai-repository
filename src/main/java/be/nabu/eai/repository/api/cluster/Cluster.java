package be.nabu.eai.repository.api.cluster;

import java.util.List;

public interface Cluster {
	public List<ClusterMember> getMembers();
	public ClusterMemberSubscription addMembershipListener(ClusterMemberSubscriber subscriber);
}

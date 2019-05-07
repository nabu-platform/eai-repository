package be.nabu.eai.repository.api.cluster;

public interface ClusterMemberSubscriber {
	public void memberRemoved(ClusterMember member);
	public void memberAdded(ClusterMember member);
}

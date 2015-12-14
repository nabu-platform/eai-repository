package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.services.MultipleServiceRuntimeTracker;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;

public class EAIRepositoryServiceTrackerProvider implements ServiceRuntimeTrackerProvider {

	private EAIResourceRepository repository;

	public EAIRepositoryServiceTrackerProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		List<ServiceRuntimeTracker> trackers = new ArrayList<ServiceRuntimeTracker>();
		for (ServiceRuntimeTrackerProvider trackerProvider : repository.getDynamicRuntimeTrackers()) {
			ServiceRuntimeTracker tracker = trackerProvider.getTracker(runtime);
			if (tracker != null) {
				trackers.add(tracker);
			}
		}
		for (ServiceRuntimeTrackerProvider trackerProvider : repository.getArtifactsThatImplement(ServiceRuntimeTrackerProvider.class)) {
			ServiceRuntimeTracker tracker = trackerProvider.getTracker(runtime);
			if (tracker != null) {
				trackers.add(tracker);
			}
		}
		if (trackers.isEmpty()) {
			return null;
		}
		else if (trackers.size() == 1) {
			return trackers.get(0);
		}
		else {
			return new MultipleServiceRuntimeTracker(trackers.toArray(new ServiceRuntimeTracker[trackers.size()]));
		}
	}

	public EAIResourceRepository getRepository() {
		return repository;
	}
}

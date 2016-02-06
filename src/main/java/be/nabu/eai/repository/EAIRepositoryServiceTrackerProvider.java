package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ModifiableServiceRuntimeTrackerProvider;
import be.nabu.libs.services.MultipleServiceRuntimeTracker;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;

public class EAIRepositoryServiceTrackerProvider implements ModifiableServiceRuntimeTrackerProvider {

	private EAIResourceRepository repository;
	private List<RuntimeRegistration> registrations = new ArrayList<RuntimeRegistration>();

	public EAIRepositoryServiceTrackerProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		List<ServiceRuntimeTracker> trackers = new ArrayList<ServiceRuntimeTracker>();
		// first check the ones that were dynamically inserted into this specific run
		for (RuntimeRegistration registration : registrations) {
			// if it's the service, start tracking
			if (runtime.getService().equals(registration.getService())) {
				trackers.add(registration.getTracker());
			}
			// non-recursive, just check next
			else if (!registration.isRecursive()) {
				continue;
			}
			// we need to recursively check
			else {
				ServiceRuntime runtimeToCheck = runtime.getParent();
				while (runtimeToCheck != null) {
					if (runtimeToCheck.getService().equals(registration.getService())) {
						trackers.add(registration.getTracker());
					}
				}
			}
		}
		// then check those that were dynamically inserted into the entire repository
		for (ServiceRuntimeTrackerProvider trackerProvider : repository.getDynamicRuntimeTrackers()) {
			ServiceRuntimeTracker tracker = trackerProvider.getTracker(runtime);
			if (tracker != null) {
				trackers.add(tracker);
			}
		}
		// then check the static artifacts
		for (ServiceRuntimeTrackerProvider trackerProvider : repository.getArtifacts(ServiceRuntimeTrackerProvider.class)) {
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

	@Override
	public void addTracker(Service service, ServiceRuntimeTracker runtimeTracker, boolean recursive) {
		registrations.add(new RuntimeRegistration(service, runtimeTracker, recursive));
	}
	
	private static class RuntimeRegistration {
		private Service service;
		private ServiceRuntimeTracker tracker;
		private boolean recursive;
		public RuntimeRegistration(Service service, ServiceRuntimeTracker tracker, boolean recursive) {
			this.service = service;
			this.tracker = tracker;
			this.recursive = recursive;
		}
		public Service getService() {
			return service;
		}
		public ServiceRuntimeTracker getTracker() {
			return tracker;
		}
		public boolean isRecursive() {
			return recursive;
		}
	}
}

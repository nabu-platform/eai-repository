/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ModifiableServiceRuntimeTrackerProvider;
import be.nabu.libs.services.MultipleServiceRuntimeTracker;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
import be.nabu.libs.services.api.ServiceWrapper;

public class EAIRepositoryServiceTrackerProvider implements ModifiableServiceRuntimeTrackerProvider {

	private EAIResourceRepository repository;
	private List<RuntimeRegistration> registrations = new ArrayList<RuntimeRegistration>();

	public EAIRepositoryServiceTrackerProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	public static Service getService(ServiceRuntime runtime) {
		return resolveService(runtime.getService());
	}

	private static Service resolveService(Service service) {
		while (service instanceof ServiceWrapper) {
			service = ((ServiceWrapper) service).getOriginal();
		}
		return service;
	}
	
	private static boolean shouldTrack(ServiceRuntime runtime, String idToTrack) {
		Service running = getService(runtime);
		return running instanceof DefinedService ? ((DefinedService) running).getId().equals(idToTrack) : false;
	}
	
	@Override
	public ServiceRuntimeTracker getTracker(ServiceRuntime runtime) {
		List<ServiceRuntimeTracker> trackers = new ArrayList<ServiceRuntimeTracker>();
		// first check the ones that were dynamically inserted into this specific run
		for (RuntimeRegistration registration : registrations) {
			String serviceToTrack = ((DefinedService) registration.getService()).getId();
			// if it's the service, start tracking
			if (shouldTrack(runtime, serviceToTrack)) {
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
					if (shouldTrack(runtimeToCheck, serviceToTrack)) {
						trackers.add(registration.getTracker());
					}
					runtimeToCheck = runtimeToCheck.getParent();
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

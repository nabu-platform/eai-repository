package be.nabu.eai.repository.api;

import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;

public interface ModifiableServiceRuntimeTrackerProvider extends ServiceRuntimeTrackerProvider {
	/**
	 * Add a tracker for a service
	 * The recursive means the that it will also be applied if the service is in the call stack
	 */
	public void addTracker(Service service, ServiceRuntimeTracker runtimeTracker, boolean recursive);
}

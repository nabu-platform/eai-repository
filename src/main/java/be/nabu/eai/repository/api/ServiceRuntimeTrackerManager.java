package be.nabu.eai.repository.api;

import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;

public interface ServiceRuntimeTrackerManager extends ServiceRuntimeTrackerProvider {
	/**
	 * Once a runtime tracker has been received for a service, do we want to keep it for "nested" service calls?
	 */
	public boolean isRecursive();
}

package be.nabu.eai.repository.util;

import be.nabu.eai.services.api.FlatServiceTracker;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.pojo.POJOUtils;

public class FlatServiceTrackerWrapper implements ServiceRuntimeTracker {

	private FlatServiceTracker tracker;

	public FlatServiceTrackerWrapper(Service service, ExecutionContext context) {
		// force an empty tracker to prevent recursive tracker calls
		this.tracker = POJOUtils.newProxy(FlatServiceTracker.class, service, context);
	}
	
	public FlatServiceTrackerWrapper(FlatServiceTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public void error(Service service, Exception exception) {
		if (service instanceof DefinedService) {
			tracker.track(false, ((DefinedService) service).getId(), null, exception);
		}
	}

	@Override
	public void error(Object step, Exception exception) {
		if (step instanceof String) {
			tracker.track(false, null, (String) step, exception);
		}
	}

	@Override
	public void start(Service service) {
		if (service instanceof DefinedService) {
			tracker.track(true, ((DefinedService) service).getId(), null, null);
		}
	}

	@Override
	public void before(Object step) {
		if (step instanceof String) {
			tracker.track(true, null, (String) step, null);
		}
	}

	@Override
	public void stop(Service service) {
		if (service instanceof DefinedService) {
			tracker.track(false, ((DefinedService) service).getId(), null, null);
		}
	}

	@Override
	public void after(Object step) {
		if (step instanceof String) {
			tracker.track(false, null, (String) step, null);
		}
	}

	@Override
	public void report(Object arg0) {
		// do nothing
	}
	
}

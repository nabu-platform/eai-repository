package be.nabu.eai.repository.util;

import be.nabu.eai.services.api.FlatServiceTracker;
import be.nabu.libs.services.EmptyServiceRuntimeTracker;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.services.vm.api.Step;
import be.nabu.libs.services.vm.api.VMServiceRuntimeTracker;

public class FlatServiceTrackerWrapper implements VMServiceRuntimeTracker {

	private FlatServiceTracker tracker;

	public FlatServiceTrackerWrapper(Service service, ExecutionContext context) {
		// force an empty tracker to prevent recursive tracker calls
		this.tracker = POJOUtils.newProxy(FlatServiceTracker.class, service, new EmptyServiceRuntimeTracker(), context);
	}
	
	public FlatServiceTrackerWrapper(FlatServiceTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public void error(Service arg0, Exception arg1) {
		tracker.track(false, arg0 instanceof DefinedService ? ((DefinedService) arg0).getId() : null, null, arg1);
	}

	@Override
	public void error(String arg0, Exception arg1) {
		tracker.track(false, null, arg0, arg1);
	}

	@Override
	public void start(Service arg0) {
		tracker.track(true, arg0 instanceof DefinedService ? ((DefinedService) arg0).getId() : null, null, null);
	}

	@Override
	public void start(String arg0) {
		tracker.track(true, null, arg0, null);
	}

	@Override
	public void stop(Service arg0) {
		tracker.track(false, arg0 instanceof DefinedService ? ((DefinedService) arg0).getId() : null, null, null);
	}

	@Override
	public void stop(String arg0) {
		tracker.track(false, null, arg0, null);
	}

	@Override
	public void after(Step arg0) {
		// do nothing
	}

	@Override
	public void before(Step arg0) {
		// do nothing
	}

	@Override
	public void error(Step arg0, Exception arg1) {
		// do nothing
	}

	@Override
	public void report(Object arg0) {
		// do nothing
	}
	
}

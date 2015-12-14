package be.nabu.eai.repository.util;

import java.util.Stack;

import be.nabu.eai.repository.api.FlatServiceTracker;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.pojo.POJOUtils;

public class FlatServiceTrackerWrapper implements ServiceRuntimeTracker {

	private FlatServiceTracker tracker;
	private Stack<String> services = new Stack<String>();
	private boolean servicesOnly;
	
	public FlatServiceTrackerWrapper(Service service, ExecutionContext context) {
		// force an empty tracker to prevent recursive tracker calls
		this.tracker = POJOUtils.newProxy(FlatServiceTracker.class, service, context);
	}
	
	public FlatServiceTrackerWrapper(FlatServiceTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public void error(Service service, Exception exception) {
		if (!services.isEmpty() && service instanceof DefinedService) {
			tracker.track(false, ((DefinedService) service).getId(), null, exception);
			services.pop();
		}
	}

	@Override
	public void error(Object step, Exception exception) {
		if (!servicesOnly && !services.isEmpty() && step instanceof String) {
			tracker.track(false, services.peek(), (String) step, exception);
		}
	}

	@Override
	public void start(Service service) {
		if (service instanceof DefinedService) {
			tracker.track(true, ((DefinedService) service).getId(), null, null);
			services.push(((DefinedService) service).getId());
		}
	}

	@Override
	public void before(Object step) {
		if (!servicesOnly && !services.isEmpty() && step instanceof String) {
			tracker.track(true, null, (String) step, null);
		}
	}

	@Override
	public void stop(Service service) {
		if (!services.isEmpty() && service instanceof DefinedService) {
			tracker.track(false, ((DefinedService) service).getId(), null, null);
			services.pop();
		}
	}

	@Override
	public void after(Object step) {
		if (!servicesOnly && !services.isEmpty() && step instanceof String) {
			tracker.track(false, null, (String) step, null);
		}
	}

	@Override
	public void report(Object arg0) {
		// do nothing
	}

	public Stack<String> getServices() {
		return services;
	}

	public boolean isServicesOnly() {
		return servicesOnly;
	}

	public void setServicesOnly(boolean servicesOnly) {
		this.servicesOnly = servicesOnly;
	}
	
}

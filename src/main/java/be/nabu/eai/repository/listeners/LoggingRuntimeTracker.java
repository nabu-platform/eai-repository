package be.nabu.eai.repository.listeners;

import org.slf4j.LoggerFactory;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.vm.api.Step;
import be.nabu.libs.services.vm.api.VMServiceRuntimeTracker;

public class LoggingRuntimeTracker implements VMServiceRuntimeTracker {

	@Override
	public void start(Service arg0) {
		if (arg0 instanceof DefinedService) {
			String id = ((DefinedService) arg0).getId();
			LoggerFactory.getLogger(id).info("Starting service: " + id);
		}
	}

	@Override
	public void error(Service arg0, Exception arg1) {
		if (arg0 instanceof DefinedService) {
			String id = ((DefinedService) arg0).getId();
			LoggerFactory.getLogger(id).error("Error running service: " + id, arg1);
		}
	}

	@Override
	public void stop(Service arg0) {
		
	}

	@Override
	public void error(String arg0, Exception arg1) {
		
	}

	@Override
	public void start(String arg0) {
		
	}


	@Override
	public void stop(String arg0) {
		
	}

	@Override
	public void after(Step arg0) {
		
	}

	@Override
	public void before(Step arg0) {
		
	}

}

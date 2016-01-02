package be.nabu.eai.repository;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.api.ComplexType;

public class ServiceInterfaceFromDefinedService implements DefinedServiceInterface {

	private DefinedService service;

	public ServiceInterfaceFromDefinedService(DefinedService service) {
		this.service = service;
	}
	
	@Override
	public ComplexType getInputDefinition() {
		return service.getServiceInterface().getInputDefinition();
	}

	@Override
	public ComplexType getOutputDefinition() {
		return service.getServiceInterface().getOutputDefinition();
	}

	@Override
	public ServiceInterface getParent() {
		return service.getServiceInterface().getParent();
	}

	@Override
	public String getId() {
		return service.getId();
	}
}

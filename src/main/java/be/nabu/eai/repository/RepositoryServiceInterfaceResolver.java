package be.nabu.eai.repository;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;
import be.nabu.libs.services.api.ServiceInterface;

public class RepositoryServiceInterfaceResolver implements DefinedServiceInterfaceResolver {

	private Repository repository;
	
	public RepositoryServiceInterfaceResolver(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedServiceInterface resolve(String id) {
		Artifact resolved = repository.resolve(id);
		if (resolved instanceof DefinedServiceInterface) {
			return (DefinedServiceInterface) resolved;
		}
		else if (resolved instanceof DefinedService) {
			ServiceInterface iface = ((DefinedService) resolved).getServiceInterface();
			if (!(iface instanceof DefinedServiceInterface)) {
				iface = new ServiceInterfaceFromDefinedService((DefinedService) resolved);
			}
			return (DefinedServiceInterface) iface;
		}
		return null;
	}
}

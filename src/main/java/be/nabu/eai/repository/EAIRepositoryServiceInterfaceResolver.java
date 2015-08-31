package be.nabu.eai.repository;

import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;

public class EAIRepositoryServiceInterfaceResolver implements DefinedServiceInterfaceResolver {

	private EAIResourceRepository repository;
	
	public EAIRepositoryServiceInterfaceResolver(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedServiceInterface resolve(String id) {
		return (DefinedServiceInterface) repository.resolve(id);
	}
}

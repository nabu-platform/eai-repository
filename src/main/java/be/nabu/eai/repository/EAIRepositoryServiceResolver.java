package be.nabu.eai.repository;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceResolver;

public class EAIRepositoryServiceResolver implements DefinedServiceResolver {

	private EAIResourceRepository repository;
	
	public EAIRepositoryServiceResolver(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedService resolve(String id) {
		return (DefinedService) repository.resolve(id);
	}
}

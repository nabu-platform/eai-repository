package be.nabu.eai.repository;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceResolver;

public class EAIRepositoryServiceResolver implements DefinedServiceResolver {

	private EAIRepository repository;
	
	public EAIRepositoryServiceResolver(EAIRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedService resolve(String id) {
		return (DefinedService) repository.resolve(id);
	}
}

package be.nabu.eai.repository;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceResolver;

public class RepositoryServiceResolver implements DefinedServiceResolver {

	private Repository repository;
	
	public RepositoryServiceResolver(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedService resolve(String id) {
		return (DefinedService) repository.resolve(id);
	}
}

package be.nabu.eai.repository.resources;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.resources.api.ResourceContainer;

abstract public class RepositoryResource implements ResourceContainer<RepositoryResource> {

	private Repository repository;
	private RepositoryEntry parent;
	private String name;
	
	public RepositoryResource(Repository repository, RepositoryEntry parent, String name) {
		this.name = name;
		this.repository = repository;
		this.parent = parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RepositoryEntry getParent() {
		return parent;
	}

	public Repository getRepository() {
		return repository;
	}
}

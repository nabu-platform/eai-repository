package be.nabu.eai.repository;

import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.util.NodeUtils;
import be.nabu.libs.services.api.CacheDecider;
import be.nabu.libs.services.api.DefinedService;

public class RepositoryCacheDecider implements CacheDecider {

	private Repository repository;

	public RepositoryCacheDecider(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public long getCacheTimeout(DefinedService service) {
		Node node = getNode(service);
		return node != null && NodeUtils.getCacheTimeout(node) != null ? NodeUtils.getCacheTimeout(node) : 0;
	}

	@Override
	public boolean shouldCache(DefinedService service) {
		Node node = getNode(service);
		return node != null && NodeUtils.getCacheTimeout(node) != null;
	}

	@Override
	public boolean shouldRefresh(DefinedService service) {
		Node node = getNode(service);
		return node != null && NodeUtils.shouldRefreshCache(node);
	}

	private Node getNode(DefinedService service) {
		return repository.getNode(service.getId());
	}
}

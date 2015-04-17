package be.nabu.eai.repository;

import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.util.NodeUtils;
import be.nabu.libs.services.api.CacheDecider;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;

public class RepositoryCacheDecider implements CacheDecider {

	private Repository repository;

	public RepositoryCacheDecider(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public long getCacheTimeout(Service arg0) {
		Node node = getNode(arg0);
		return node != null && NodeUtils.getCacheTimeout(node) != null ? NodeUtils.getCacheTimeout(node) : 0;
	}

	@Override
	public boolean shouldCache(Service arg0) {
		Node node = getNode(arg0);
		return node != null && NodeUtils.getCacheTimeout(node) != null;
	}

	@Override
	public boolean shouldRefresh(Service arg0) {
		Node node = getNode(arg0);
		return node != null && NodeUtils.shouldRefreshCache(node);
	}

	private Node getNode(Service service) {
		return service instanceof DefinedService ? repository.getNode(((DefinedService) service).getId()) : null;
	}
}

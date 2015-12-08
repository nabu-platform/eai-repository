package be.nabu.eai.repository.managers;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.proxy.DefinedProxy;
import be.nabu.eai.repository.artifacts.proxy.ProxyConfiguration;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class ProxyManager extends JAXBArtifactManager<ProxyConfiguration, DefinedProxy> {

	public ProxyManager() {
		super(DefinedProxy.class);
	}

	@Override
	protected DefinedProxy newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DefinedProxy(id, container, repository);
	}

}

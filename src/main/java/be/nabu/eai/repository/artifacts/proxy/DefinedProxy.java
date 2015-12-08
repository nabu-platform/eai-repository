package be.nabu.eai.repository.artifacts.proxy;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class DefinedProxy extends JAXBArtifact<ProxyConfiguration> {

	public DefinedProxy(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "proxy.xml", ProxyConfiguration.class);
	}

}

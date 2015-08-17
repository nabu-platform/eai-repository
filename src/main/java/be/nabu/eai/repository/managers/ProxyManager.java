package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.proxy.DefinedProxy;
import be.nabu.eai.repository.artifacts.proxy.ProxyConfiguration;
import be.nabu.eai.repository.managers.util.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;

public class ProxyManager extends JAXBArtifactManager<ProxyConfiguration, DefinedProxy> {

	public ProxyManager() {
		super(DefinedProxy.class);
	}

	@Override
	protected DefinedProxy newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DefinedProxy(id, container);
	}

	@Override
	public List<String> getReferences(DefinedProxy artifact) throws IOException {
		return null;
	}

	@Override
	public List<Validation<?>> updateReference(DefinedProxy artifact, String from, String to) throws IOException {
		return null;
	}

}

package be.nabu.eai.repository.managers;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServerConfiguration;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class DefinedHTTPServerManager extends JAXBArtifactManager<DefinedHTTPServerConfiguration, DefinedHTTPServer> {

	public DefinedHTTPServerManager() {
		super(DefinedHTTPServer.class);
	}

	@Override
	protected DefinedHTTPServer newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DefinedHTTPServer(id, container);
	}

}

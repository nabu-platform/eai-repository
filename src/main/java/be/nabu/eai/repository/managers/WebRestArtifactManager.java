package be.nabu.eai.repository.managers;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.web.rest.WebRestArtifact;
import be.nabu.eai.repository.artifacts.web.rest.WebRestArtifactConfiguration;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WebRestArtifactManager extends JAXBArtifactManager<WebRestArtifactConfiguration, WebRestArtifact> {

	public WebRestArtifactManager() {
		super(WebRestArtifact.class);
	}

	@Override
	protected WebRestArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WebRestArtifact(id, container);
	}

}

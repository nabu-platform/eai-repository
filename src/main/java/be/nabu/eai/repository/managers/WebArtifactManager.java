package be.nabu.eai.repository.managers;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.web.WebArtifact;
import be.nabu.eai.repository.artifacts.web.WebArtifactConfiguration;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WebArtifactManager extends JAXBArtifactManager<WebArtifactConfiguration, WebArtifact> {

	public WebArtifactManager() {
		super(WebArtifact.class);
	}

	@Override
	protected WebArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WebArtifact(id, container, repository);
	}

}

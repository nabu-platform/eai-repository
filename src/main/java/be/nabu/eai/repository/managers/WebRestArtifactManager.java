package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.web.rest.WebRestArtifact;
import be.nabu.eai.repository.artifacts.web.rest.WebRestArtifactConfiguration;
import be.nabu.eai.repository.managers.util.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.validator.api.Validation;

public class WebRestArtifactManager extends JAXBArtifactManager<WebRestArtifactConfiguration, WebRestArtifact> {

	public WebRestArtifactManager() {
		super(WebRestArtifact.class);
	}

	@Override
	public List<String> getReferences(WebRestArtifact artifact) throws IOException {
		return null;
	}

	@Override
	public List<Validation<?>> updateReference(WebRestArtifact artifact, String from, String to) throws IOException {
		return null;
	}

	@Override
	protected WebRestArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WebRestArtifact(id, container);
	}

}

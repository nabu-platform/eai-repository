package be.nabu.eai.repository.artifacts.web;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.resources.api.ResourceContainer;

public class PermissionsArtifact extends JAXBArtifact<WebArtifactConfiguration> implements PermissionHandler {

	public PermissionsArtifact(String id, ResourceContainer<?> directory,
			Repository repository, String fileName,
			Class<WebArtifactConfiguration> configurationClazz) {
		super(id, directory, repository, fileName, configurationClazz);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean hasPermission(Token token, String context, String action) {
		// TODO Auto-generated method stub
		return false;
	}

}

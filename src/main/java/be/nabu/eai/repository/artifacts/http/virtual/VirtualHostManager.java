package be.nabu.eai.repository.artifacts.http.virtual;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class VirtualHostManager extends JAXBArtifactManager<VirtualHostConfiguration, VirtualHostArtifact> {

	public VirtualHostManager() {
		super(VirtualHostArtifact.class);
	}

	@Override
	protected VirtualHostArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new VirtualHostArtifact(id, container, repository);
	}
	
}

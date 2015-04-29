package be.nabu.eai.repository.managers.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

abstract public class JAXBArtifactManager<C, T extends JAXBArtifact<C>> implements ArtifactManager<T> {

	private Class<T> artifactClass;

	public JAXBArtifactManager(Class<T> artifactClass) {
		this.artifactClass = artifactClass;
	}
	
	abstract protected T newInstance(String id, ResourceContainer<?> container, Repository repository);
	
	@Override
	public T load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		return newInstance(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, T artifact) throws IOException {
		try {
			artifact.save(entry.getContainer());
		}
		catch (IOException e) {
			return Arrays.asList(new ValidationMessage(Severity.ERROR, "Could not save " + artifactClass.getSimpleName() + ": " + e.getMessage()));
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return null;
	}

	@Override
	public Class<T> getArtifactClass() {
		return artifactClass;
	}

}

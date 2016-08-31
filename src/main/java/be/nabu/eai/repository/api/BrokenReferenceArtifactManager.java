package be.nabu.eai.repository.api;

import java.io.IOException;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.validator.api.Validation;

public interface BrokenReferenceArtifactManager<T extends Artifact> extends ArtifactManager<T> {
	public List<Validation<?>> updateBrokenReference(ResourceContainer<?> container, String from, String to) throws IOException;
}

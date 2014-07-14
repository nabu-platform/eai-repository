package be.nabu.eai.repository.api;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.ResourceContainer;

public interface ArtifactManager<T extends Artifact> {
	public T load(ResourceContainer<?> container);
	public void save(T artifact, ResourceContainer<?> container);
	public Class<T> getArtifactClass();
}

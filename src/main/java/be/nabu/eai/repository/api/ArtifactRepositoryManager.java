package be.nabu.eai.repository.api;

import java.io.IOException;

import be.nabu.libs.artifacts.api.Artifact;

public interface ArtifactRepositoryManager<T extends Artifact> extends ArtifactManager<T> {
	public void addChildren(ModifiableEntry parent, T artifact) throws IOException; 
}

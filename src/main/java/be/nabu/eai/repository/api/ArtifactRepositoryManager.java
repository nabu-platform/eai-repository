package be.nabu.eai.repository.api;

import java.io.IOException;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface ArtifactRepositoryManager<T extends Artifact> extends ArtifactManager<T> {
	public List<Entry> addChildren(ModifiableEntry parent, T artifact) throws IOException;
	public List<Entry> removeChildren(ModifiableEntry parent, T artifact) throws IOException;
}

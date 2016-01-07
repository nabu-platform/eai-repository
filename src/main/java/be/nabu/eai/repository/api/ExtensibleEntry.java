package be.nabu.eai.repository.api;

import java.io.IOException;

import be.nabu.eai.repository.resources.RepositoryEntry;

public interface ExtensibleEntry {
	public RepositoryEntry createDirectory(String name) throws IOException;
	public RepositoryEntry createNode(String name, ArtifactManager<?> manager, boolean reload) throws IOException;
	public void deleteChild(String name, boolean recursive) throws IOException;
}

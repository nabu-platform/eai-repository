package be.nabu.eai.repository.api;

import java.nio.charset.Charset;

import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.events.api.EventDispatcher;

public interface Repository extends ArtifactResolver<Artifact> {
	public RepositoryEntry getRoot();
	public Charset getCharset();
	public EventDispatcher getEventDispatcher();
	public Node getNode(String id);
}

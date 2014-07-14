package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface Node {
	public List<String> getReferences();
	public Artifact getArtifact();
	public Class<? extends Artifact> getArtifactClass();
	public boolean isLeaf();
	
	@SuppressWarnings("rawtypes")
	public Class<? extends ArtifactManager> getArtifactManager();
}

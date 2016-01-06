package be.nabu.eai.repository.api;

import java.util.Collection;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.LazyArtifact;

public interface ContainerArtifact extends LazyArtifact {
	public Collection<Artifact> getContainedArtifacts();
	public Map<String, String> getConfiguration(Artifact child);
	public void addArtifact(String part, Artifact artifact, Map<String, String> configuration);
	public String getPartName(Artifact artifact);
}

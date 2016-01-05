package be.nabu.eai.repository.api;

import java.util.List;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;

public interface ContainerArtifact extends Artifact {
	public List<Artifact> getContainedArtifacts();
	public Map<String, String> getConfiguration(Artifact child);
	public void addArtifact(Artifact artifact, Map<String, String> configuration);
}

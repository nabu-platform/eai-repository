package be.nabu.eai.repository.artifacts.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.api.ContainerArtifact;
import be.nabu.libs.artifacts.api.Artifact;

public class BaseContainerArtifact implements ContainerArtifact {

	private String id;
	private List<Artifact> artifacts = new ArrayList<Artifact>();
	private Map<Artifact, Map<String, String>> configurations = new HashMap<Artifact, Map<String,String>>();

	public BaseContainerArtifact(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public List<Artifact> getContainedArtifacts() {
		return artifacts;
	}

	@Override
	public Map<String, String> getConfiguration(Artifact child) {
		return configurations.get(child);
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends Artifact> T getArtifact(Class<T> clazz) {
		for (Artifact artifact : artifacts) {
			if (clazz.isAssignableFrom(artifact.getClass())) {
				return (T) artifact;
			}
		}
		return null;
	}

	@Override
	public void addArtifact(Artifact artifact, Map<String, String> configuration) {
		configurations.put(artifact, configuration);
		artifacts.add(artifact);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Artifact> T getArtifact(String name) {
		for (Artifact artifact : artifacts) {
			if (artifact.getId().endsWith(":" + name)) {
				return (T) artifact;
			}
		}
		return null;
	}
}

package be.nabu.eai.repository.api;

import be.nabu.libs.artifacts.api.Artifact;

public interface VariableRefactorArtifactManager<T extends Artifact> extends ArtifactManager<T> {
	public boolean updateVariableName(T artifact, Artifact type, String oldPath, String newPath);
}

package be.nabu.eai.repository.api;

import be.nabu.libs.artifacts.api.Artifact;

public interface CreatableArtifactFragmentManager<T extends Artifact> extends ArtifactFragmentManager<T> {
	public Entry createArtifact(Entry parent, String name);
}

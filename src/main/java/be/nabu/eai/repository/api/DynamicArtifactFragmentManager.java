package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.Validation;

public interface DynamicArtifactFragmentManager<T extends Artifact> extends ArtifactFragmentManager<T> {
	public List<Validation<?>> createFragment(T artifact, String path, String initialContent);
	public List<Validation<?>> deleteFragment(T artifact, String path);
}

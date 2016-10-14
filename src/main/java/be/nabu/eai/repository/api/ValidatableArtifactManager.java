package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.Validation;

public interface ValidatableArtifactManager<T extends Artifact> extends ArtifactManager<T> {
	public List<? extends Validation<?>> validate(T instance);
}

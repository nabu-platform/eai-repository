package be.nabu.eai.repository.api;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.ValidationMessage;

public interface ArtifactManager<T extends Artifact> {
	public T load(ResourceEntry<?> entry) throws IOException, ParseException;
	public List<ValidationMessage> save(ResourceEntry<?> entry, T artifact) throws IOException;
	public Class<T> getArtifactClass();
}

package be.nabu.eai.repository.api;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.Validation;

public interface ArtifactManager<T extends Artifact> {
	/**
	 * The messages list allows you to return warnings/errors on load
	 */
	public T load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException;
	public List<Validation<?>> save(ResourceEntry entry, T artifact) throws IOException;
	public Class<T> getArtifactClass();
	public List<String> getReferences(T artifact) throws IOException;
	public List<Validation<?>> updateReference(T artifact, String from, String to) throws IOException;
}

package be.nabu.eai.repository.api;

import java.io.InputStream;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface ClassProvidingArtifact extends Artifact {
	public List<Class<?>> getImplementationsFor(Class<?> clazz);
	public Class<?> loadClass(String id);
	public InputStream loadResource(String id);
}

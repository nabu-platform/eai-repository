package be.nabu.eai.repository.simple;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.SimpleTypeWrapper;

public class RepositoryTypeWrapper implements SimpleTypeWrapper {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> DefinedSimpleType<T> wrap(Class<T> arg0) {
		if (Artifact.class.isAssignableFrom(arg0)) {
			return new ArtifactSimpleType(arg0);
		}
		else if (Class.class.isAssignableFrom(arg0)) {
			return (DefinedSimpleType<T>) new ClassSimpleType();
		}
		else {
			return null;
		}
	}

	@Override
	public DefinedSimpleType<?> getByName(String name) {
		return null;
	}

}

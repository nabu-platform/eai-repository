package be.nabu.eai.repository.simple;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.SimpleTypeWrapper;

public class ArtifactTypeWrapper implements SimpleTypeWrapper {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> DefinedSimpleType<T> wrap(Class<T> arg0) {
		return Artifact.class.isAssignableFrom(arg0) ? new ArtifactSimpleType(arg0) : null;
	}

}

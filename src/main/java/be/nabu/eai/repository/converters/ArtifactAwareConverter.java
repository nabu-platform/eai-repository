package be.nabu.eai.repository.converters;

import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.converter.api.Converter;

public class ArtifactAwareConverter implements Converter {

	@Override
	public boolean canConvert(Class<?> instanceClass, Class<?> targetClass) {
		if (Artifact.class.isAssignableFrom(instanceClass) && String.class.equals(targetClass)) {
			return true;
		}
		else if (Artifact.class.isAssignableFrom(targetClass) && String.class.equals(instanceClass)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Class<T> targetClass) {
		if (instance instanceof String && Artifact.class.isAssignableFrom(targetClass)) {
			return (T) ArtifactResolverFactory.getInstance().getResolver().resolve((String) instance);
		}
		else if (instance instanceof Artifact && String.class.equals(targetClass)) {
			return (T) ((Artifact) instance).getId();
		}
		return null;
	}
	
}

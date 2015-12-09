package be.nabu.eai.repository.simple;

import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.BaseMarshallableSimpleType;

public class ArtifactSimpleType<T extends Artifact> extends BaseMarshallableSimpleType<T> implements Unmarshallable<T> {

	private ArtifactResolver<?> resolver;
	
	public ArtifactSimpleType(Class<T> artifactClass) {
		super(artifactClass);
	}

	@Override
	public String marshal(T arg0, Value<?>...arg1) {
		return arg0 == null ? null : arg0.getId();
	}
	
	@Override
	public String getName(Value<?>... arg0) {
		return "artifact";
	}

	@Override
	public String getNamespace(Value<?>... arg0) {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T unmarshal(String arg0, Value<?>... arg1) {
		return (T) (arg0 == null ? null : getResolver().resolve(arg0));
	}

	public ArtifactResolver<?> getResolver() {
		return resolver == null ? ArtifactResolverFactory.getInstance().getResolver() : resolver;
	}

	public void setResolver(ArtifactResolver<?> resolver) {
		this.resolver = resolver;
	}
}

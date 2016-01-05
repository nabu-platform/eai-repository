package be.nabu.eai.repository.artifacts.simpleType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.PropertyFactory;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.converters.StringToSimpleType;
import be.nabu.libs.types.properties.EnumerationProperty;
import be.nabu.libs.validator.api.Validator;

public class SimpleTypeArtifact<T> extends JAXBArtifact<SimpleTypeConfiguration> implements DefinedSimpleType<T>, Marshallable<T>, Unmarshallable<T> {

	public SimpleTypeArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "simpleType.xml", SimpleTypeConfiguration.class);
	}

	@SuppressWarnings("unchecked")
	public SimpleType<T> getParent() {
		try {
			return new StringToSimpleType().convert(getConfiguration().getParent() != null && !getId().equals(getConfiguration().getParent()) ? getConfiguration().getParent() : "java.lang.String");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String marshal(T object, Value<?>... values) {
		return object == null ? null : ((Marshallable<T>) getParent()).marshal(object);
	}
	
	@Override
	public T unmarshal(String content, Value<?>... values) {
		return content == null ? null : ((Unmarshallable<T>) getParent()).unmarshal(content, values);
	}

	@Override
	public String getName(Value<?>... values) {
		return getParent().getName();
	}

	@Override
	public String getNamespace(Value<?>... values) {
		return getParent().getNamespace();
	}

	@Override
	public Class<T> getInstanceClass() {
		return getParent().getInstanceClass();
	}

	@Override
	public boolean isList(Value<?>... properties) {
		return getParent().isList(properties);
	}

	@Override
	public Validator<?> createValidator(Value<?>... properties) {
		return getParent().createValidator(properties);
	}

	@Override
	public Validator<Collection<?>> createCollectionValidator(Value<?>... properties) {
		return getParent().createCollectionValidator(properties);
	}

	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>... properties) {
		return getParent().getSupportedProperties(properties);
	}

	@Override
	public Type getSuperType() {
		return getParent();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Value<?>[] getProperties() {
		Map<Property<?>, Value<?>> properties = new LinkedHashMap<Property<?>, Value<?>>();
		try {
			if (getConfiguration().getProperties() != null && !getConfiguration().getProperties().isEmpty()) {
				for (String propertyName : getConfiguration().getProperties().keySet()) {
					Property<?> property = PropertyFactory.getInstance().getProperty(propertyName);
					if (property != null) {
						String string = getConfiguration().getProperties().get(propertyName);
						properties.put(property, new ValueImpl(property, ConverterFactory.getInstance().getConverter().convert(string, property.getValueClass())));
					}
				}
			}
			if (getConfiguration().getEnumerations() != null && !getConfiguration().getEnumerations().isEmpty()) {
				EnumerationProperty<T> enumerationProperty = new EnumerationProperty<T>();
				List<T> values = new ArrayList<T>();
				for (String enumeration : getConfiguration().getEnumerations()) {
					values.add(ConverterFactory.getInstance().getConverter().convert(enumeration, getParent().getInstanceClass()));
				}
				properties.put(enumerationProperty, new ValueImpl<List<T>>(enumerationProperty, values));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return properties.values().toArray(new Value[0]);
	}

}

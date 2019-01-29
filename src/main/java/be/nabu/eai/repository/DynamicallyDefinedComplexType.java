package be.nabu.eai.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Group;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validator;

public class DynamicallyDefinedComplexType implements ComplexType, DefinedType {
	
	private String id;
	private ComplexType type;

	public DynamicallyDefinedComplexType(String id, ComplexType type) {
		this.id = id;
		this.type = type;
	}
	
	@Override
	public String getName(Value<?>...values) {
		return type.getName(values);
	}

	@Override
	public String getNamespace(Value<?>... values) {
		return type.getNamespace(values);
	}

	@Override
	public boolean isList(Value<?>... properties) {
		return type.isList(properties);
	}

	@Override
	public Validator<?> createValidator(Value<?>... properties) {
		return type.createValidator(properties);
	}

	@Override
	public Validator<Collection<?>> createCollectionValidator(Value<?>... properties) {
		return type.createCollectionValidator(properties);
	}

	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>... properties) {
		return type.getSupportedProperties(properties);
	}

	@Override
	public Type getSuperType() {
		return type.getSuperType();
	}

	@Override
	public Value<?>[] getProperties() {
		return type.getProperties();
	}

	@Override
	public Iterator<Element<?>> iterator() {
		return type.iterator();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Element<?> get(String path) {
		return type.get(path);
	}

	@Override
	public ComplexContent newInstance() {
		return new DynamicallyDefinedComplexContent(type, this);
	}

	@Override
	public Boolean isAttributeQualified(Value<?>... values) {
		return type.isAttributeQualified(values);
	}

	@Override
	public Boolean isElementQualified(Value<?>... values) {
		return type.isElementQualified(values);
	}

	@Override
	public Group[] getGroups() {
		return type.getGroups();
	}
}
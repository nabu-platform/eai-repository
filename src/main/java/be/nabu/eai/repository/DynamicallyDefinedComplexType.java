/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
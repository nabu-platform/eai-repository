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

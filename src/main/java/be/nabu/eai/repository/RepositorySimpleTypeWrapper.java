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

import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.SimpleTypeWrapper;

public class RepositorySimpleTypeWrapper implements SimpleTypeWrapper {

	private Repository repository;

	public RepositorySimpleTypeWrapper(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public <T> DefinedSimpleType<T> wrap(Class<T> object) {
		return null;
	}

	@Override
	public DefinedSimpleType<?> getByName(String name) {
		if (name.startsWith("[")) {
			return null;
		}
		Node node = repository.getNode(name);
		if (node != null && DefinedSimpleType.class.isAssignableFrom(node.getArtifactClass())) {
			try {
				return (DefinedSimpleType<?>) node.getArtifact();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

}

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

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeResolver;

public class RepositoryTypeResolver implements DefinedTypeResolver {

	private Repository repository;
	
	public RepositoryTypeResolver(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedType resolve(String id) {
		String part = null;
		int index = id.indexOf(':');
		if (index > 0) {
			part = id.substring(index + 1);
			id = id.substring(0, index);
		}
		Artifact artifact = repository.resolve(id);
		if (artifact instanceof DefinedType) {
			return (DefinedType) artifact;
		}
		else if (artifact instanceof DefinedService) {
			if ("input".equals(part)) {
				return new DynamicallyDefinedComplexType(id + ":" + part, ((DefinedService) artifact).getServiceInterface().getInputDefinition());
			}
			else if ("output".equals(part)) {
				return new DynamicallyDefinedComplexType(id + ":" + part, ((DefinedService) artifact).getServiceInterface().getOutputDefinition());
			}
		}
		return null;
	}
}

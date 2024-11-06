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

package be.nabu.eai.repository.resources;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.resources.api.ResourceContainer;

abstract public class RepositoryResource implements ResourceContainer<RepositoryResource> {

	private Repository repository;
	private RepositoryEntry parent;
	private String name;
	
	public RepositoryResource(Repository repository, RepositoryEntry parent, String name) {
		this.name = name;
		this.repository = repository;
		this.parent = parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RepositoryEntry getParent() {
		return parent;
	}

	public Repository getRepository() {
		return repository;
	}
}

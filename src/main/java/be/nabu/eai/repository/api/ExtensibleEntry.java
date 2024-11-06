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

package be.nabu.eai.repository.api;

import java.io.IOException;

import be.nabu.eai.repository.resources.RepositoryEntry;

public interface ExtensibleEntry extends Entry {
	public RepositoryEntry createDirectory(String name) throws IOException;
	public RepositoryEntry createNode(String name, ArtifactManager<?> manager, boolean reload) throws IOException;
	public void deleteChild(String name, boolean recursive) throws IOException;
}

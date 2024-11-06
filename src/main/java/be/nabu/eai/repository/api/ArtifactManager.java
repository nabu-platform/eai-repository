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
import java.text.ParseException;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.Validation;

public interface ArtifactManager<T extends Artifact> {
	/**
	 * The messages list allows you to return warnings/errors on load
	 */
	public T load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException;
	public List<Validation<?>> save(ResourceEntry entry, T artifact) throws IOException;
	public Class<T> getArtifactClass();
	public List<String> getReferences(T artifact) throws IOException;
	public List<Validation<?>> updateReference(T artifact, String from, String to) throws IOException;
}

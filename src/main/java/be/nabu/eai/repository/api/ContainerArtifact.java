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

import java.util.Collection;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.LazyArtifact;

public interface ContainerArtifact extends LazyArtifact {
	public Collection<Artifact> getContainedArtifacts();
	public Map<String, String> getConfiguration(Artifact child);
	public void addArtifact(String part, Artifact artifact, Map<String, String> configuration);
	public String getPartName(Artifact artifact);
}

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

package be.nabu.eai.repository.artifacts.container;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import be.nabu.eai.repository.api.ContainerArtifact;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.LazyArtifact;

public class BaseContainerArtifact implements ContainerArtifact {

	private String id;
	/**
	 * The insertion order is important as it should dictate how the references are resolved internally
	 */
	private Map<String, Artifact> artifacts = new LinkedHashMap<String, Artifact>();
	private Map<Artifact, Map<String, String>> configurations = new HashMap<Artifact, Map<String,String>>();

	public BaseContainerArtifact(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public Collection<Artifact> getContainedArtifacts() {
		return artifacts.values();
	}

	@Override
	public Map<String, String> getConfiguration(Artifact child) {
		return configurations.get(child);
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends Artifact> T getArtifact(Class<T> clazz) {
		for (Artifact artifact : artifacts.values()) {
			if (clazz.isAssignableFrom(artifact.getClass())) {
				return (T) artifact;
			}
		}
		return null;
	}

	@Override
	public void addArtifact(String name, Artifact artifact, Map<String, String> configuration) {
		if (configuration != null) {
			// always set the current id, a lot of things need this
			configuration.put("actualId", getId());
			configurations.put(artifact, configuration);
		}
		artifacts.put(name, artifact);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Artifact> T getArtifact(String name) {
		return (T) artifacts.get(name);
	}

	@Override
	public String getPartName(Artifact artifact) {
		for (String key : artifacts.keySet()) {
			if (artifact.equals(artifacts.get(key))) {
				return key;
			}
		}
		return artifact.getId().replaceAll("^.*:", "");
	}
	
	public void removeArtifact(String name) {
		if (artifacts.containsKey(name)) {
			configurations.remove(artifacts.get(name));
			artifacts.remove(name);
		}
	}

	@Override
	public void forceLoad() {
		for (Artifact artifact : artifacts.values()) {
			if (artifact instanceof LazyArtifact) {
				((LazyArtifact) artifact).forceLoad();
			}
		}
	}
	
}

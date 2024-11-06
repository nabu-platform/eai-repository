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

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.util.ClassAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;

@XmlRootElement(name = "container")
public class ContainerArtifactConfiguration {
	
	private Class<?> artifactManagerClass;
	private Map<String, String> configuration;
	
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getConfiguration() {
		return configuration;
	}
	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}
	
	@XmlJavaTypeAdapter(ClassAdapter.class)
	public Class<?> getArtifactManagerClass() {
		return artifactManagerClass;
	}
	public void setArtifactManagerClass(Class<?> artifactManagerClass) {
		this.artifactManagerClass = artifactManagerClass;
	}
}

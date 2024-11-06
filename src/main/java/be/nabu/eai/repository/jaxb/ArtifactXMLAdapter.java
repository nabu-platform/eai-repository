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

package be.nabu.eai.repository.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;

public class ArtifactXMLAdapter extends XmlAdapter<String, Artifact> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ArtifactResolver<?> resolver;
	
	public ArtifactXMLAdapter(ArtifactResolver<?> resolver) {
		this.resolver = resolver;
	}
	
	public ArtifactXMLAdapter() {
		this(ArtifactResolverFactory.getInstance().getResolver());
	}
	
	@Override
	public Artifact unmarshal(String v) throws Exception {
		try {
			return v == null ? null : resolver.resolve(v);
		}
		catch (Exception e) {
			logger.error("Could not load artifact: " + v, e);
			return null;
		}
	}

	@Override
	public String marshal(Artifact v) throws Exception {
		return v == null ? null : v.getId();
	}

}

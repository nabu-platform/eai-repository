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

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.resources.VirtualContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceResolver;

public class RepositoryResourceResolver implements ResourceResolver {
	
	private static List<String> defaultSchemes = Arrays.asList(new String [] { "repository" });
	private ResourceRepository repository;
	
	public RepositoryResourceResolver(ResourceRepository repository) {
		this.repository = repository;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Resource getResource(URI uri, Principal principal) {
		try {
			URI child = new URI(URIUtils.encodeURI(uri.getSchemeSpecificPart()));
			Entry entry = repository.getEntry(child.getScheme());
			if (entry == null) {
				throw new IllegalArgumentException("Non-existing node: " + child.getScheme());
			}
			else if (!(entry instanceof ResourceEntry)) {
				throw new IllegalArgumentException("The entry is not resource-based: " + child.getScheme());
			}
			Resource privateFolder = ((ResourceEntry) entry).getContainer().getChild(EAIResourceRepository.PRIVATE);
			Resource publicFolder = ((ResourceEntry) entry).getContainer().getChild(EAIResourceRepository.PUBLIC);
			Resource protectedFolder = ((ResourceEntry) entry).getContainer().getChild(EAIResourceRepository.PROTECTED);
			VirtualContainer container = new VirtualContainer(new URI(uri.getScheme() + ":" + child.getScheme() + ":/"));
			if (privateFolder != null) {
				container.addChild(EAIResourceRepository.PRIVATE, privateFolder);
			}
			if (publicFolder != null) {
				container.addChild(EAIResourceRepository.PUBLIC, publicFolder);
			}
			if (protectedFolder != null) {
				container.addChild(EAIResourceRepository.PROTECTED, protectedFolder);
			}
			// the entry is is part of the scheme which means the path is always relative to the given entry (meaning it can never point to a child entry)
			// child entries must never have any of the reserved names but conversely any non-reserved name could point to a child entry
			// which means a non-reserved name can never be a valid part of the entry filesystem you want to expose as it belongs to the repository hierarchy
			// which makes it safer to use that reserved names which may be retooled for specific purposes at some point
			if (entry.isNode() && Resource.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
				container.addChild("artifact", (Resource) entry.getNode().getArtifact());
			}
			return ResourceUtils.resolve(container, child.getPath());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public List<String> getDefaultSchemes() {
		return defaultSchemes;
	}
	

}

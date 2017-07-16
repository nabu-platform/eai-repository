package be.nabu.eai.repository.resources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
			try {
				return ResourceUtils.resolve(container, child.getPath());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public List<String> getDefaultSchemes() {
		return defaultSchemes;
	}
	

}

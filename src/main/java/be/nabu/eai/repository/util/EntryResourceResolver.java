package be.nabu.eai.repository.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.types.xml.ResourceResolver;
import be.nabu.utils.io.IOUtils;

public class EntryResourceResolver implements ResourceResolver {

	private ResourceEntry root;

	public EntryResourceResolver(ResourceEntry root) {
		this.root = root;
	}
	
	@Override
	public InputStream resolve(URI uri) throws IOException {
		if (uri.getScheme() != null) {
			throw new IOException("The uri for the managed client wsdl should be local");
		}
		String path = uri.getPath();
		if (path.startsWith("/")) {
			throw new IOException("The uri for the managed client wsdl should be relative");
		}
		ParsedPath parsed = new ParsedPath(path);
		ResourceEntry entry = root;
		while (parsed.getChildPath() != null) {
			entry = (ResourceEntry) entry.getChild(parsed.getName());
			if (entry == null) {
				throw new FileNotFoundException("Could not find the folder " + parsed.getName());
			}
			parsed = parsed.getChildPath();
		}
		Resource resource = entry.getContainer().getChild(parsed.getName());
		if (resource == null) {
			throw new FileNotFoundException("Could not find the file " + parsed.getName());
		}
		return IOUtils.toInputStream(new ResourceReadableContainer((ReadableResource) resource));
	}

	@Override
	public TypeRegistry resolve(String namespace) throws IOException {
		throw new IllegalStateException("Can not resolve namespaces in an entry resolver");
	}
	
}
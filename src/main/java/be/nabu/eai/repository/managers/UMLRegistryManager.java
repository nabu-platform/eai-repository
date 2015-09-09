package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.managers.base.TypeRegistryManager;
import be.nabu.eai.repository.util.EntryResourceResolver;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.types.uml.UMLRegistry;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.xml.XMLUtils;

public class UMLRegistryManager extends TypeRegistryManager<UMLRegistry> {

	public UMLRegistryManager() {
		super(UMLRegistry.class);
	}

	@Override
	public UMLRegistry load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		try {
			UMLRegistry registry = new UMLRegistry(entry.getId());
			registry.setResourceResolver(new EntryResourceResolver(entry));
			// always load the base types first
			InputStream input = UMLRegistry.class.getClassLoader().getResourceAsStream("baseTypes.xmi");
			try {
				registry.load(XMLUtils.toDocument(input, true));
			}
			finally {
				input.close();
			}
			List<Document> documents = new ArrayList<Document>();
			for (Resource child : entry.getContainer()) {
				if (child.getName().endsWith(".xmi") && child instanceof ReadableResource) {
					ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) child);
					try {
						documents.add(XMLUtils.toDocument(IOUtils.toInputStream(readable), true));
					}
					finally {
						readable.close();
					}
				}
			}
			// load all existing xmi files together (because we don't know the order they should be loaded in)
			registry.load(documents.toArray(new Document[documents.size()]));
			return registry;
		}
		catch (SAXException e) {
			throw new ParseException(e.getMessage(), 0);
		}
		catch (ParserConfigurationException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}

}

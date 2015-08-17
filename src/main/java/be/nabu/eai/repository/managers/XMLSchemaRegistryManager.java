package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.xml.sax.SAXException;

import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.managers.WSDLClientManager.EntryResourceResolver;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.types.xml.XMLSchema;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class XMLSchemaRegistryManager extends TypeRegistryManager<XMLSchema> {

	public XMLSchemaRegistryManager() {
		super(XMLSchema.class);
	}

	@Override
	public XMLSchema load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Resource resource = entry.getContainer().getChild("schema.xsd");
		if (resource == null) {
			throw new FileNotFoundException("Can not find schema.xsd");
		}
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			XMLSchema schema = new XMLSchema(IOUtils.toInputStream(readable));
			schema.setId(entry.getId());
			schema.setResolver(new EntryResourceResolver(entry));
			schema.parse();
			return schema;
		}
		catch (SAXException e) {
			throw new ParseException(e.getMessage(), 0);
		}
		finally {
			readable.close();
		}
	}

}

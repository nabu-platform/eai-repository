package be.nabu.eai.repository.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class StructureManager implements ArtifactManager<DefinedStructure> {

	@Override
	public DefinedStructure load(ResourceEntry<?> entry) throws IOException, ParseException {
		Resource resource = entry.getContainer().getChild("structure.xml");
		if (resource == null) {
			throw new FileNotFoundException("Can not find structure.xml");
		}
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			XMLDefinitionUnmarshaller unmarshaller = new XMLDefinitionUnmarshaller();
			// evil!
			DefinedStructure structure = (DefinedStructure) unmarshaller.unmarshal(IOUtils.toInputStream(readable));
			structure.setId(entry.getId());
			return structure;
		}
		finally {
			readable.close();
		}
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry<?> entry, DefinedStructure artifact) throws IOException {
		Resource resource = entry.getContainer().getChild("structure.xml");
		if (resource == null) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create("structure.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) resource);
		try {
			XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
			marshaller.marshal(IOUtils.toOutputStream(writable), artifact);
			return new ArrayList<ValidationMessage>();
		}
		finally {
			writable.close();
		}
	}

	@Override
	public Class<DefinedStructure> getArtifactClass() {
		return DefinedStructure.class;
	}

}

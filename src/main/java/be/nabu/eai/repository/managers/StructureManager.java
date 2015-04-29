package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableType;
import be.nabu.libs.types.api.ModifiableTypeInstance;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.SuperTypeProperty;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class StructureManager implements ArtifactManager<DefinedStructure> {

	@Override
	public DefinedStructure load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		DefinedStructure structure = (DefinedStructure) parse(entry, "structure.xml");
		structure.setId(entry.getId());
		return structure;
	}

	public static Structure parse(ResourceEntry entry, String name) throws FileNotFoundException, IOException, ParseException {
		Resource resource = entry.getContainer().getChild(name);
		if (resource == null) {
			throw new FileNotFoundException("Can not find structure.xml");
		}
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			XMLDefinitionUnmarshaller unmarshaller = new XMLDefinitionUnmarshaller();
			unmarshaller.setIdToUnmarshal(entry.getId());
			// evil cast!
			Structure structure = (Structure) unmarshaller.unmarshal(IOUtils.toInputStream(readable));
			return structure;
		}
		finally {
			readable.close();
		}
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, DefinedStructure artifact) throws IOException {
		List<ValidationMessage> messages = format(entry, artifact, "structure.xml");
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return messages;
	}

	public static List<ValidationMessage> format(ResourceEntry entry, ComplexType artifact, String name) throws IOException {
		Resource resource = entry.getContainer().getChild(name);
		if (resource == null) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create(name, "application/xml");
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

	@Override
	public List<String> getReferences(DefinedStructure artifact) throws IOException {
		return getComplexReferences(artifact);
	}
	
	public static List<String> getComplexReferences(ComplexType type) {
		List<String> references = new ArrayList<String>();
		getReferences(type, references);
		return references;
	}
	
	private static void getReferences(ComplexType type, List<String> references) {
		if (type.getSuperType() != null && type.getSuperType() instanceof Artifact) {
			String id = ((Artifact) type.getSuperType()).getId();
			if (!references.contains(id)) {
				references.add(id);
			}
		}
		// only local children, don't loop over supertype children
		for (Element<?> child : type) {
			// if it is a reference, don't recurse
			if (child.getType() instanceof Artifact) {
				Artifact artifact = (Artifact) child.getType();
				if (!references.contains(artifact.getId())) {
					references.add(artifact.getId());
				}
			}
			else if (child.getType() instanceof ComplexType) {
				getReferences((ComplexType) child.getType(), references);
			}
		}
	}
	
	public static List<ValidationMessage> updateReferences(ComplexType type, String from, String to) {
		List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
		if (type.getSuperType() != null && type.getSuperType() instanceof Artifact) {
			String id = ((Artifact) type.getSuperType()).getId();
			if (from.equals(id)) {
				if (!(type instanceof ModifiableType)) {
					messages.add(new ValidationMessage(Severity.ERROR, "Could not update supertype from '" + from + "' to '" + to + "'"));
				}
				else {
					Artifact newSuperType = ArtifactResolverFactory.getInstance().getResolver().resolve(to);
					if (!(newSuperType instanceof Type)) {
						messages.add(new ValidationMessage(Severity.ERROR, "Not a type: " + to));	
					}
					else {
						((ModifiableType) type).setProperty(new ValueImpl<Type>(SuperTypeProperty.getInstance(), (Type) newSuperType));
					}
				}
			}
		}
		for (Element<?> child : type) {
			if (child.getType() instanceof Artifact) {
				Artifact artifact = (Artifact) child.getType();
				if (from.equals(artifact.getId())) {
					if (!(child instanceof ModifiableTypeInstance)) {
						messages.add(new ValidationMessage(Severity.ERROR, "Could not update referenced type from '" + from + "' to '" + to + "'"));	
					}
					else {
						Artifact newType = ArtifactResolverFactory.getInstance().getResolver().resolve(to);
						if (!(newType instanceof Type)) {
							messages.add(new ValidationMessage(Severity.ERROR, "Not a type: " + to));	
						}
						else {
							((ModifiableTypeInstance) child).setType((Type) newType);
						}
					}
				}
			}
			else if (child.getType() instanceof ComplexType) {
				messages.addAll(updateReferences((ComplexType) child.getType(), from, to));
			}
		}
		return messages;
	}

	@Override
	public List<ValidationMessage> updateReference(DefinedStructure artifact, String from, String to) throws IOException {
		return updateReferences(artifact, from, to);
	}

}

package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ModifiableServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.PipelineInterfaceProperty;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.types.structure.SuperTypeProperty;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class ServiceInterfaceManager implements ArtifactManager<DefinedServiceInterface> {

	public Pipeline loadPipeline(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		// we need to load the pipeline which is basically a structure
		XMLDefinitionUnmarshaller unmarshaller = new XMLDefinitionUnmarshaller();
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) VMServiceManager.getResource(entry, "pipeline.xml", false));
		Pipeline pipeline = new Pipeline(null, null);
		try {
			unmarshaller.unmarshal(IOUtils.toInputStream(readable), pipeline);
		}
		finally {
			readable.close();
		}
		DefinedServiceInterface value = ValueUtils.getValue(PipelineInterfaceProperty.getInstance(), pipeline.getProperties());
		// if we have an interface
		if (value != null) {
			pipeline.get(Pipeline.INPUT).setProperty(new ValueImpl<Type>(SuperTypeProperty.getInstance(), value.getInputDefinition()));
			pipeline.get(Pipeline.OUTPUT).setProperty(new ValueImpl<Type>(SuperTypeProperty.getInstance(), value.getOutputDefinition()));
		}
		return pipeline;
	}
	
	@Override
	public DefinedServiceInterface load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		Pipeline pipeline = loadPipeline(entry, messages);
		return new DefinedServiceInterfaceImpl(entry.getId(), pipeline);
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, DefinedServiceInterface artifact) throws IOException {
		Pipeline pipeline = artifact instanceof DefinedServiceInterfaceImpl 
			? ((DefinedServiceInterfaceImpl) artifact).pipeline
			: new Pipeline(artifact.getInputDefinition(), artifact.getOutputDefinition());
		savePipeline(entry, pipeline);
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return new ArrayList<ValidationMessage>();
	}

	public void savePipeline(ResourceEntry entry, Pipeline pipeline) throws IOException {
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) VMServiceManager.getResource(entry, "pipeline.xml", true));
		try {
			XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
			marshaller.setIgnoreUnknownSuperTypes(true);
			marshaller.marshal(IOUtils.toOutputStream(writable), pipeline);
		}
		finally {
			writable.close();
		}
	}
	
	@Override
	public Class<DefinedServiceInterface> getArtifactClass() {
		return DefinedServiceInterface.class;
	}
	
	public static class DefinedServiceInterfaceImpl implements DefinedServiceInterface {

		private Pipeline pipeline;
		private String id;

		public DefinedServiceInterfaceImpl(String id, Pipeline pipeline) {
			this.id = id;
			this.pipeline = pipeline;
		}
		
		@Override
		public ComplexType getInputDefinition() {
			return (ComplexType) pipeline.get(Pipeline.INPUT).getType();
		}

		@Override
		public ComplexType getOutputDefinition() {
			return (ComplexType) pipeline.get(Pipeline.OUTPUT).getType();
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public ServiceInterface getParent() {
			return ValueUtils.getValue(PipelineInterfaceProperty.getInstance(), pipeline.getProperties());
		}
		
	}

	@Override
	public List<String> getReferences(DefinedServiceInterface artifact) throws IOException {
		return getReferencesForInterface(artifact);
	}

	public static List<String> getReferencesForInterface(ServiceInterface artifact) {
		List<String> references = new ArrayList<String>();
		if (artifact.getParent() instanceof Artifact) {
			references.add(((Artifact) artifact.getParent()).getId());
		}
		references.addAll(StructureManager.getComplexReferences(artifact.getInputDefinition()));
		references.addAll(StructureManager.getComplexReferences(artifact.getOutputDefinition()));
		return references;
	}

	@Override
	public List<ValidationMessage> updateReference(DefinedServiceInterface artifact, String from, String to) throws IOException {
		return updateReferences(artifact, from, to);
	}

	public static List<ValidationMessage> updateReferences(ServiceInterface artifact, String from, String to) {
		List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
		if (artifact.getParent() instanceof Artifact) {
			String id = ((Artifact) artifact.getParent()).getId();
			if (from.equals(id)) {
				if (!(artifact instanceof ModifiableServiceInterface)) {
					messages.add(new ValidationMessage(Severity.ERROR, "The service interface is not modifiable"));
				}
				else {
					Artifact newParent = ArtifactResolverFactory.getInstance().getResolver().resolve(to);
					if (!(newParent instanceof DefinedServiceInterface)) {
						messages.add(new ValidationMessage(Severity.ERROR, "Not a service interface: " + to));	
					}
					else {
						((ModifiableServiceInterface) artifact).setParent((DefinedServiceInterface) newParent);
					}
				}
			}
		}
		messages.addAll(StructureManager.updateReferences(artifact.getInputDefinition(), from, to));
		messages.addAll(StructureManager.updateReferences(artifact.getOutputDefinition(), from, to));
		return messages;
	}
}

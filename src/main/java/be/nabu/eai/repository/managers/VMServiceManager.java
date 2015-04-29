package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.SimpleExecutionContext.SimpleServiceContext;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.SimpleVMServiceDefinition;
import be.nabu.libs.services.vm.api.Step;
import be.nabu.libs.services.vm.api.StepGroup;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.services.vm.step.Invoke;
import be.nabu.libs.services.vm.step.Sequence;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class VMServiceManager implements ArtifactManager<VMService> {

	public static Resource getResource(ResourceEntry entry, String name, boolean create) throws IOException {
		Resource resource = entry.getContainer().getChild(name);
		if (resource == null && create) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create(name, "application/xml");
		}
		if (resource == null) {
			throw new FileNotFoundException("Can not find " + name);
		}
		return resource;
	}
	
	@Override
	public VMService load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		Pipeline pipeline = new ServiceInterfaceManager().loadPipeline(entry, messages);
		// next we load the root sequence
		XMLBinding sequenceBinding = new XMLBinding((ComplexType) BeanResolver.getInstance().resolve(Sequence.class), Charset.forName("UTF-8"));
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) getResource(entry, "service.xml", false));
		Sequence sequence = null;
		try {
			sequence = TypeUtils.getAsBean(sequenceBinding.unmarshal(IOUtils.toInputStream(readable), new Window[0]), Sequence.class);
		}
		finally {
			readable.close();
		}
		
		SimpleVMServiceDefinition definition = new SimpleVMServiceDefinition(pipeline);
		definition.setRoot(sequence);
		definition.setId(entry.getId());
		return definition;
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, VMService artifact) throws IOException {
		new ServiceInterfaceManager().savePipeline(entry, artifact.getPipeline());
		
		// next we load the root sequence
		XMLBinding sequenceBinding = new XMLBinding((ComplexType) BeanResolver.getInstance().resolve(Sequence.class), Charset.forName("UTF-8"));
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) getResource(entry, "service.xml", true));
		try {
			sequenceBinding.marshal(IOUtils.toOutputStream(writable), new BeanInstance<Sequence>(artifact.getRoot()));
		}
		finally {
			writable.close();
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return artifact.getRoot().validate(new SimpleServiceContext());		
	}

	@Override
	public Class<VMService> getArtifactClass() {
		return VMService.class;
	}

	@Override
	public List<String> getReferences(VMService artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		// all the type references (including input and output) are in the pipeline
		references.addAll(StructureManager.getComplexReferences(artifact.getPipeline()));
		// another reference are all the services that are invoked
		references.addAll(getReferencesForStep(artifact.getRoot()));
		return references;
	}
	
	public static List<String> getReferencesForStep(StepGroup steps) {
		List<String> references = new ArrayList<String>();
		for (Step step : steps.getChildren()) {
			if (step instanceof Invoke) {
				String id = ((Invoke) step).getServiceId();
				if (!references.contains(id)) {
					references.add(id);
				}
			}
			if (step instanceof StepGroup) {
				references.addAll(getReferencesForStep((StepGroup) step));
			}
		}
		return references;
	}
	
	public static void updateReferences(StepGroup steps, String from, String to) {
		for (Step step : steps.getChildren()) {
			if (step instanceof Invoke) {
				if (from.equals(((Invoke) step).getServiceId())) {
					((Invoke) step).setServiceId(to);
				}
			}
			if (step instanceof StepGroup) {
				updateReferences((StepGroup) step, from, to);
			}
		}
	}

	@Override
	public List<ValidationMessage> updateReference(VMService artifact, String from, String to) throws IOException {
		List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
		messages.addAll(StructureManager.updateReferences(artifact.getPipeline(), from, to));
		updateReferences(artifact.getRoot(), from, to);
		return messages;
	}

}

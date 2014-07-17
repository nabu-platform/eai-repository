package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.SimpleServiceRuntime.SimpleServiceContext;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.Sequence;
import be.nabu.libs.services.vm.SimpleVMServiceDefinition;
import be.nabu.libs.services.vm.VMService;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class VMServiceManager implements ArtifactManager<VMService> {

	private Resource getResource(ResourceEntry<?> entry, String name, boolean create) throws IOException {
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
	public VMService load(ResourceEntry<?> entry) throws IOException, ParseException {
		// we need to load the pipeline which is basically a structure
		XMLDefinitionUnmarshaller unmarshaller = new XMLDefinitionUnmarshaller();
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) getResource(entry, "pipeline.xml", false));
		Pipeline pipeline = new Pipeline(null, null);
		try {
			unmarshaller.unmarshal(IOUtils.toInputStream(readable), pipeline);
		}
		finally {
			readable.close();
		}
		// next we load the root sequence
		XMLBinding sequenceBinding = new XMLBinding(new BeanType<Sequence>(Sequence.class), Charset.forName("UTF-8"));
		readable = new ResourceReadableContainer((ReadableResource) getResource(entry, "service.xml", false));
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
	public List<ValidationMessage> save(ResourceEntry<?> entry, VMService artifact) throws IOException {
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) getResource(entry, "pipeline.xml", true));
		try {
			XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
			marshaller.marshal(IOUtils.toOutputStream(writable), artifact.getPipeline());
		}
		finally {
			writable.close();
		}
		
		// next we load the root sequence
		XMLBinding sequenceBinding = new XMLBinding(new BeanType<Sequence>(Sequence.class), Charset.forName("UTF-8"));
		writable = new ResourceWritableContainer((WritableResource) getResource(entry, "service.xml", true));
		try {
			sequenceBinding.marshal(IOUtils.toOutputStream(writable), new BeanInstance<Sequence>(artifact.getRoot()));
		}
		finally {
			writable.close();
		}
		return artifact.getRoot().validate(new SimpleServiceContext());		
	}

	@Override
	public Class<VMService> getArtifactClass() {
		return VMService.class;
	}

}

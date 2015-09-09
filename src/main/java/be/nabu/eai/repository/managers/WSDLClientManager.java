package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.managers.base.WSDLClient;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.eai.repository.util.EntryResourceResolver;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.services.wsdl.WSDLOperation;
import be.nabu.libs.services.wsdl.WSDLService;
import be.nabu.libs.services.wsdl.WSDLWrapper;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class WSDLClientManager implements ArtifactRepositoryManager<WSDLClient> {

	@Override
	public WSDLClient load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Resource resource = entry.getContainer().getChild("interface.wsdl");
		if (resource == null) {
			throw new FileNotFoundException("Can not find interface.wsdl");
		}
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			WSDLWrapper wrapper = new WSDLWrapper(IOUtils.toInputStream(readable), false);
			wrapper.setResolver(new EntryResourceResolver(entry));
			wrapper.parse();
			WSDLClient client = new WSDLClient(entry.getId());
			client.setWrapper(wrapper);
			return client;
		}
		catch (SAXException e) {
			throw new ParseException(e.getMessage(), 0);
		}
		finally {
			readable.close();
		}
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, WSDLClient artifact) throws IOException {
		throw new IOException("Can not update a client wsdl this way");
	}

	@Override
	public Class<WSDLClient> getArtifactClass() {
		return WSDLClient.class;
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry parent, WSDLClient artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		((EAINode) parent.getNode()).setLeaf(false);
		
		MemoryEntry services = new MemoryEntry(parent.getRepository(), parent, null, parent.getId() + ".services", "services");
		for (WSDLOperation operation : artifact.getWrapper().getOperations()) {
			WSDLService service = new WSDLService(services.getId() + "." + operation.getName(), operation);
			EAINode node = new EAINode();
			node.setArtifact(service);
			node.setLeaf(true);
			MemoryEntry entry = new MemoryEntry(services.getRepository(), services, node, services.getId() + "." + operation.getName(), operation.getName());
			node.setEntry(entry);
			services.addChildren(entry);
			entries.add(entry);
		}
		// TODO: add documents!
		parent.addChildren(services);
		return entries;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, WSDLClient artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		Entry services = parent.getChild("services");
		if (services != null) {
			for (Entry service : services) {
				entries.add(service);
			}
			parent.removeChildren("services");
		}
		// TODO: add documents!
		return entries;
	}

	@Override
	public List<String> getReferences(WSDLClient artifact) throws IOException {
		return null;
	}

	@Override
	public List<Validation<?>> updateReference(WSDLClient artifact, String from, String to) throws IOException {
		return null;
	}
}

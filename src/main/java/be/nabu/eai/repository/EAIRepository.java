package be.nabu.eai.repository;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.events.NodeEvent;
import be.nabu.eai.repository.events.NodeEvent.State;
import be.nabu.libs.events.EventDispatcherImpl;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanType;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

/**
 * Ideally at some point I want to plug in the higher level VFS api instead of the low level resource API
 * To make this easier, I have tried to encapsulate as much resource-related stuff here as possible
 */
public class EAIRepository {
	
	private ManageableContainer<?> root;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	
	public static final String PRIVATE = "private";
	public static final String PUBLIC = "public";
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static List<String> INTERNAL = Arrays.asList(new String [] { PRIVATE, PUBLIC });
	private Charset charset = Charset.forName("UTF-8");
	
	public EAIRepository(ManageableContainer<?> root) {
		this.root = root;
	}
	
	public ManageableContainer<?> getResourceContainer(String id) throws IOException {
		String path = id.replace('.', '/');
		Resource resource = ResourceUtils.resolve(root, path);
		if (resource == null) {
			resource = ResourceUtils.mkdirs(root, path);
		}
		return (ManageableContainer<?>) resource;
	}
	
	public Resource getResource(String id, String...paths) throws IOException {
		StringBuilder builder = new StringBuilder();
		for (String path : paths) {
			if (!builder.toString().isEmpty()) {
				builder.append("/");
			}
			builder.append(path);
		}
		String path = builder.toString();
		Resource resource = ResourceUtils.resolve(getResourceContainer(id), path);
		if (resource == null) {
			String mimeType = ContentTypeMap.getInstance().getContentTypeFor(path);
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			int index = path.lastIndexOf('/');
			if (index <= 0) {
				resource = getResourceContainer(id).create(path, mimeType);
			}
			else {
				String parent = path.substring(0, index);
				path = path.substring(index + 1);
				Resource parentResource = ResourceUtils.resolve(root, parent);
				if (parentResource == null) {
					parentResource = ResourceUtils.mkdirs(parentResource, parent);
				}
				resource = ((ManageableContainer<?>) parentResource).create(path, mimeType);
			}
		}
		return resource;
	}
	
	public void load() {
		load(null, root);
	}
	
	private void load(String path, ResourceContainer<?> container) {
		boolean recurse = true;
		// don't allow nodes in the root, no way to name them
		if (path != null) {
			dispatcher.fire(new NodeEvent(path, State.LOAD, false), this);
			// check if there is a "node.xml" file, if so it is a resource
			Resource nodeResource = container.getChild("node.xml");
			if (nodeResource != null) {
				XMLBinding binding = new XMLBinding(new BeanType<EAINode>(EAINode.class), charset);
				try {
					ReadableContainer<ByteBuffer> readable = ((ReadableResource) nodeResource).getReadable();
					EAINode node = TypeUtils.getAsBean(binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]), EAINode.class);
					// TODO: build reference & dependency map
					recurse = !node.isLeaf();
				}
				catch (IOException e) {
					logger.error("Could not load node " + path, e);
				}
				catch (ParseException e) {
					logger.error("Could not load node " + path, e);
				}
			}
			dispatcher.fire(new NodeEvent(path, State.LOAD, true), this);
		}
		if (recurse) {
			for (Resource child : container) {
				if (child instanceof ResourceContainer) {
					if (!INTERNAL.contains(child.getName())) {
						load(path == null ? child.getName() : path + "." + child.getName(), (ResourceContainer<?>) child);
					}
				}
			}
		}
	}

	public EventDispatcher getDispatcher() {
		return dispatcher;
	}
}

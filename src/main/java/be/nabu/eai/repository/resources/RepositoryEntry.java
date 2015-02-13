package be.nabu.eai.repository.resources;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.events.NodeEvent;
import be.nabu.eai.repository.events.NodeEvent.State;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * A repository entry is basically a container that can either contain a node, child entries or both
 * In the simplest case this maps to a "folder" on the file system
 */
public class RepositoryEntry implements ResourceEntry, ModifiableEntry {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private ResourceContainer<?> container;
	private RepositoryEntry parent;
	private String name;
	private Date lastLoaded;
	private EAINode node;
	private ResourceRepository repository;
	
	private Map<String, Entry> children;
	
	public RepositoryEntry(ResourceRepository repository, ResourceContainer<?> container, RepositoryEntry parent, String name) {
		this.repository = repository;
		this.container = container;
		this.parent = parent;
		this.name = name;
	}
	
	@Override
	public String getContentType() {
		return isNode()
			? "application/vnd-nabu-" + getNode().getArtifactClass().getName()
			: Resource.CONTENT_TYPE_DIRECTORY;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RepositoryEntry getParent() {
		return parent;
	}

	@Override
	public Iterator<Entry> iterator() {
		return getChildren().values().iterator();
	}

	@Override
	public Entry getChild(String name) {
		// some elements have an id with upper camelcase for the artifact but the entry has lowercase camelcase
		// this fixes that
		// note that this is in sync with the creation policy set by the server where the artifact name must begin with a small letter
		name = name.length() <= 1 ? name.toLowerCase() : name.substring(0, 1).toLowerCase() + name.substring(1);
		return getChildren().get(name);
	}
	
	@Override
	public void refresh() {
		children = null;
	}
	
	private Map<String, Entry> getChildren() {
		if (children == null) {
			children = new LinkedHashMap<String, Entry>();
			if (!isLeaf()) {
				for (Resource child : container) {
					if (child instanceof ResourceContainer && !repository.isInternal(container)) {
						children.put(child.getName(), new RepositoryEntry(repository, (ResourceContainer<?>) child, this, child.getName()));
					}
				}
			}
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return isNode() && getNode().isLeaf();
	}
	
	/**
	 * For production we can cache this check, for development it's best not to so you can continuously pick up new nodes (or deleted ones)
	 */
	public boolean isNode() {
		return container.getChild("node.xml") != null;
	}
	
	public RepositoryEntry createDirectory(String name) throws IOException {
		if (!getRepository().isValidName(getContainer(), name)) {
			throw new IOException("Invalid name: " + name);
		}
		ManageableContainer<?> newDirectory = (ManageableContainer<?>) ((ManageableContainer<?>) getContainer()).create(name, Resource.CONTENT_TYPE_DIRECTORY);
		RepositoryEntry entry = new RepositoryEntry(getRepository(), newDirectory, this, name);
		children.put(name, entry);
		return entry;
	}
	
	public RepositoryEntry createNode(String name, ArtifactManager<?> manager) throws IOException {
		if (getRepository().isValidName(getContainer(), name)) {
			ManageableContainer<?> nodeContainer = (ManageableContainer<?>) ((ManageableContainer<?>) getContainer()).create(name, Resource.CONTENT_TYPE_DIRECTORY);
			EAINode node = new EAINode();
			node.setArtifactManager(manager.getClass());
			node.setLeaf(true);
			XMLBinding binding = new XMLBinding((ComplexType) BeanResolver.getInstance().resolve(EAINode.class), repository.getCharset());
			Resource target = nodeContainer.create("node.xml", "application/xml");
			WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
			try {
				binding.marshal(IOUtils.toOutputStream(writable), new BeanInstance<EAINode>(node));
			}
			finally {
				writable.close();
			}
			RepositoryEntry entry = new RepositoryEntry(getRepository(), nodeContainer, this, name);
			children.put(name, entry);
			return entry;
		}
		else {
			throw new IOException("Invalid name: " + name);
		}
	}
	
	public Node getNode() {
		Resource resource = container.getChild("node.xml");
		if (resource != null && (node == null || lastLoaded == null || (resource instanceof TimestampedResource && ((TimestampedResource) resource).getLastModified().after(lastLoaded)))) {
			boolean isReload = false;
			if (node != null) {
				isReload = true;
				repository.getEventDispatcher().fire(new NodeEvent(getId(), node, State.RELOAD, false), this);
				node = null;
			}
			else {
				repository.getEventDispatcher().fire(new NodeEvent(getId(), null, State.LOAD, false), this);
			}
			XMLBinding binding = new XMLBinding((ComplexType) BeanResolver.getInstance().resolve(EAINode.class), repository.getCharset());
			try {
				ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
				try {
					node = TypeUtils.getAsBean(binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]), EAINode.class);
					node.setEntry(this);
				}
				finally {
					readable.close();
				}
				repository.getEventDispatcher().fire(new NodeEvent(getId(), node, isReload ? State.RELOAD : State.LOAD, true), this);
			}
			catch (IOException e) {
				logger.error("Could not load node " + getId(), e);
			}
			catch (ParseException e) {
				logger.error("Could not parse node " + getId(), e);
			}

			if (resource instanceof TimestampedResource) {
				lastLoaded = ((TimestampedResource) resource).getLastModified();
			}
		}
		return node;
	}

	@Override
	public String getId() {
		String id = "";
		RepositoryEntry entry = this;
		while (entry.getParent() != null) {
			if (!id.isEmpty()) {
				id = "." + id;
			}
			id = entry.getName() + id;
			entry = entry.getParent();
		}
		return id;
	}

	@Override
	public boolean isEditable() {
		return container instanceof ManageableContainer;
	}

	@Override
	public ResourceRepository getRepository() {
		return repository;
	}

	@Override
	public ResourceContainer<?> getContainer() {
		return container;
	}

	@Override
	public void addChildren(Entry...children) {
		for (Entry child : children) {
			getChildren().put(child.getName(), child);
		}
	}

	@Override
	public void removeChildren(String...children) {
		for (String name : children) {
			getChildren().remove(name);
		}
	}

}

package be.nabu.eai.repository.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
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
import be.nabu.libs.resources.api.features.CacheableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * A repository entry is basically a container that can either contain a node, child entries or both
 * In the simplest case this maps to a "folder" on the file system
 */
public class RepositoryEntry implements ResourceEntry, ModifiableEntry, ModifiableNodeEntry {

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
		return getChildren().get(name);
	}
	
	@Override
	public void refresh(boolean recursive) {
		lastLoaded = null;
		if (container instanceof CacheableResource) {
			try {
				((CacheableResource) container).resetCache();
			}
			catch (IOException e) {
				logger.error("Could not refresh resource: " + container, e);
			}
		}
		rescan(recursive);
	}
	
	private Map<String, Entry> getChildren() {
		if (children == null) {
			rescan(false);
		}
		return children;
	}

	private void rescan(boolean refreshChildren) {
		if (children == null) {
			children = new LinkedHashMap<String, Entry>();
		}
		if (!isLeaf()) {
			List<String> existing = new ArrayList<String>(children.keySet());
			for (Resource child : container) {
				if (existing.contains(child.getName())) {
					existing.remove(child.getName());
					if (refreshChildren) {
						children.get(child.getName()).refresh(refreshChildren);
					}
				}
				else if (child instanceof ResourceContainer && !repository.isInternal(container) && !child.getName().startsWith(".")) {
					children.put(child.getName(), new RepositoryEntry(repository, (ResourceContainer<?>) child, this, child.getName()));
				}
			}
			for (String deleted : existing) {
				children.remove(deleted);
			}
		}
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
			repository.getEventDispatcher().fire(new NodeEvent(getId() + "." + name, null, State.CREATE, false), this);
			ManageableContainer<?> nodeContainer = (ManageableContainer<?>) ((ManageableContainer<?>) getContainer()).create(name, Resource.CONTENT_TYPE_DIRECTORY);
			EAINode node = new EAINode();
			node.setArtifactManager(manager.getClass());
			node.setLeaf(true);
			writeNode(nodeContainer, node);
			RepositoryEntry entry = new RepositoryEntry(getRepository(), nodeContainer, this, name);
			children.put(name, entry);
			// update the cached scan-typed list
			if (repository instanceof EAIResourceRepository) {
				((EAIResourceRepository) repository).scanForTypes(this);
			}
			repository.getEventDispatcher().fire(new NodeEvent(getId() + "." + name, node, State.CREATE, true), this);
			return entry;
		}
		else {
			throw new IOException("Invalid name: " + name);
		}
	}

	private void writeNode(ResourceContainer<?> nodeContainer, EAINode node) throws IOException {
		Resource target = nodeContainer.getChild("node.xml");
		if (target == null) {
			target = ((ManageableContainer<?>) nodeContainer).create("node.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
		try {
			getJAXBContext().createMarshaller().marshal(node, IOUtils.toOutputStream(writable));
		}
		catch (JAXBException e) {
			throw new IOException(e);
		}
		finally {
			writable.close();
		}
	}
	
	@Override
	public void updateNode(List<String> references) throws IOException {
		EAINode node = (EAINode) getNode();
		node.setReferences(references == null ? new ArrayList<String>() : references);
		node.setVersion(node.getVersion() + 1);
		writeNode(getContainer(), node);
		if (repository instanceof EAIResourceRepository) {
			((EAIResourceRepository) repository).updateReferences(getId(), references);
		}
	}
	
	public EAINode getNode() {
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
			try {
				ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
				try {
					node = (EAINode) getJAXBContext().createUnmarshaller().unmarshal(IOUtils.toInputStream(readable));
					node.setEntry(this);
				}
				catch (JAXBException e) {
					throw new IOException(e);
				}
				finally {
					readable.close();
				}
				repository.getEventDispatcher().fire(new NodeEvent(getId(), node, isReload ? State.RELOAD : State.LOAD, true), this);
			}
			catch (IOException e) {
				logger.error("Could not load node " + getId(), e);
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

	private static JAXBContext jaxbContext;
	
	public static JAXBContext getJAXBContext() {
		if (jaxbContext == null) {
			synchronized(EAINode.class) {
				if (jaxbContext == null) {
					try {
						jaxbContext = JAXBContext.newInstance(EAINode.class);
					}
					catch (JAXBException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return jaxbContext;
	}
}

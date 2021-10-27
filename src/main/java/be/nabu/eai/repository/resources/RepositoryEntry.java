package be.nabu.eai.repository.resources;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.CollectionImpl;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.DynamicEntry;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ExtensibleEntry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.events.NodeEvent;
import be.nabu.eai.repository.events.NodeEvent.State;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.api.features.CacheableResource;
import be.nabu.libs.resources.memory.MemoryDirectory;
import be.nabu.libs.resources.zip.ZIPArchive;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * A repository entry is basically a container that can either contain a node, child entries or both
 * In the simplest case this maps to a "folder" on the file system
 */
public class RepositoryEntry implements ResourceEntry, ModifiableEntry, ModifiableNodeEntry, ExtensibleEntry {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private ResourceContainer<?> container;
	private RepositoryEntry parent;
	private String name;
	private Date lastLoaded;
	private EAINode node;
	private ResourceRepository repository;
	private boolean reloadNode;
	
	private Map<String, Entry> children;
	private static boolean CACHE_MODULES = Boolean.parseBoolean(System.getProperty("be.nabu.eai.repository.cacheModules", "false"));
	
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
		return new ArrayList<Entry>(getChildren().values()).iterator();
	}

	@Override
	public Entry getChild(String name) {
		return getChildren().get(name);
	}
	
	@Override
	public void refresh(boolean recursive) {
		refresh(false, recursive);
	}
	
	public void refresh(boolean includeData, boolean recursive) {
		// in development mode we add some logs to better pinpoint the reload issue
		// it is assumed to lie here
		if (EAIResourceRepository.isDevelopment()) {
			logger.info("Refreshing node for " + getId() + " due to expliciet reloadNode request (" + lastLoaded + ") " + includeData + " / " + recursive);
		}
		lastLoaded = null;
		reloadNode = true;
		if (includeData) {
			refreshData();
		}
		rescan(recursive);
	}

	public void refreshData() {
		if (container instanceof CacheableResource) {
			try {
				((CacheableResource) container).resetCache();
			}
			catch (IOException e) {
				logger.error("Could not refresh resource: " + container, e);
			}
		}
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
		List<String> existing = new ArrayList<String>(children.keySet());
		for (Resource child : container) {
			String childName = child.getName();
			if (childName.endsWith(".nar")) {
				childName = childName.substring(0, childName.length() - ".nar".length());
			}
			if (existing.contains(childName)) {
				existing.remove(childName);
				// update the resource container, a reset cache may have triggered new entries
				if (children.get(childName) instanceof RepositoryEntry) {
					if (child.getName().endsWith(".nar")) {
						// this approach does not load the zip file into memory but this comes with some hefty I/O overhead for large modules (e.g. selenium, soapui,... modules)
						if (!CACHE_MODULES) {
							ZIPArchive archive = new ZIPArchive();
							archive.setSource(child);
							((RepositoryEntry) children.get(childName)).container = archive;
						}
						// this approach loads the zip into memory enabling fast access but the content will remain in memory for the duration of the application
						else {
							MemoryDirectory directory = new MemoryDirectory();
							try {
								ResourceUtils.unzip(child, directory);
							}
							catch (IOException e) {
								throw new RuntimeException(e);
							}
							((RepositoryEntry) children.get(childName)).container = directory;
						}
					}
					else {
						((RepositoryEntry) children.get(childName)).container = (ResourceContainer<?>) child;
					}
				}
				if (refreshChildren) {
					children.get(childName).refresh(refreshChildren);
				}
			}
			else if (child.getName().endsWith(".nar") && !child.getName().startsWith(".")) {
				if (!CACHE_MODULES) {
					ZIPArchive archive = new ZIPArchive();
					archive.setSource(child);
					children.put(childName, new RepositoryEntry(repository, archive, this, childName));
				}
				else {
					MemoryDirectory directory = new MemoryDirectory();
					try {
						ResourceUtils.unzip(child, directory);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					children.put(childName, new RepositoryEntry(repository, directory, this, childName));
				}
			}
			else if (child instanceof ResourceContainer && !EAIResourceRepository.RESERVED.contains(child.getName()) && !child.getName().startsWith(".")) {
				children.put(child.getName(), new RepositoryEntry(repository, (ResourceContainer<?>) child, this, child.getName()));
			}
		}
		for (String deleted : existing) {
			// don't auto-delete dynamic entries
			if (!(children.get(deleted) instanceof DynamicEntry)) {
				children.remove(deleted);
			}
		}
	}

	@Override
	public boolean isLeaf() {
		return isNode() && getNode().isLeaf() && getChildren().isEmpty();
	}
	
	/**
	 * For production we can cache this check, for development it's best not to so you can continuously pick up new nodes (or deleted ones)
	 */
	public boolean isNode() {
		return container.getChild("node.xml") != null;
	}
	
	@Override
	public void deleteChild(String name, boolean recursive) throws IOException {
		Entry entry = getChildren().get(name);
		if (entry != null) {
			// unload it from the repository to remove references/dependencies
			// we unload it before we delete it, otherwise we might not be able to unload it correctly...cause its already gone!
			repository.unload(entry.getId());
			// if it is resource-based, delete the files
			if (entry instanceof ResourceEntry) {
				// if recursive, just delete everything
				if (recursive) {
					((ManageableContainer<?>) getContainer()).delete(name);
					getChildren().remove(name);
				}
				// else be more specific about the deletes
				else {
					// first delete the necessary files
					List<String> toDelete = new ArrayList<String>();
					boolean hasOthers = false;
					for (Resource resource : ((ResourceEntry) entry).getContainer()) {
						if (!(resource instanceof ResourceContainer) || getRepository().isInternal((ResourceContainer<?>) resource)) {
							toDelete.add(resource.getName());
						}
						else {
							hasOthers = true;
						}
					}
					if (!hasOthers) {
						((ManageableContainer<?>) getContainer()).delete(name);
						getChildren().remove(name);	
					}
					else {
						for (String delete : toDelete) {
							((ManageableContainer<?>) ((ResourceEntry) entry).getContainer()).delete(delete);
						}
						getChildren().put(name, createDirectory(name));
					}
				}
			}
			// in memory
			else {
				getChildren().remove(name);
			}
			// make sure we see the change at the resource level
			if (getContainer() instanceof CacheableResource) {
				((CacheableResource) getContainer()).resetCache();
			}
		}
	}
	
	@Override
	public RepositoryEntry createDirectory(String name) throws IOException {
		if (!getRepository().isValidName(getContainer(), name)) {
			throw new IOException("Invalid name: " + name);
		}
		else if (getChildren().containsKey(name)) {
			Entry child = getChild(name);
			if (child instanceof RepositoryEntry) {
				if (child.isLeaf()) {
					((RepositoryEntry) child).getNode().setLeaf(false);
					// update the leaf property in repo
					writeNode(((RepositoryEntry) child).getContainer(), ((RepositoryEntry) child).getNode());
				}
				return (RepositoryEntry) child;
			}
			else {
				throw new IOException("The child already exists and is not a repository entry");
			}
		}
		else {
			ManageableContainer<?> newDirectory = (ManageableContainer<?>) ((ManageableContainer<?>) getContainer()).create(name, Resource.CONTENT_TYPE_DIRECTORY);
			RepositoryEntry entry = new RepositoryEntry(getRepository(), newDirectory, this, name);
			getChildren().put(name, entry);
			repository.reload(entry.getId());
			return entry;
		}
	}
	
	@Override
	public RepositoryEntry createNode(String name, ArtifactManager<?> manager, boolean reload) throws IOException {
		Entry child = getChild(name);
		if (child != null && child.isNode()) {
			throw new IOException("A node with the name '" + name + "' already exists");
		}
		else if (child != null && !(child instanceof RepositoryEntry)) {
			throw new IOException("A directory with the name '" + name + "' already exists and it is not a repository entry");
		}
		else if (child == null && !getRepository().isValidName(getContainer(), name)) {
			throw new IOException("The name '" + name + "' is not valid");
		}
		// only send events if we want to trigger reloads etc
		if (reload) {
			repository.getEventDispatcher().fire(new NodeEvent(getId() + "." + name, null, State.CREATE, false), this);
		}
		ManageableContainer<?> nodeContainer = child == null ? (ManageableContainer<?>) ((ManageableContainer<?>) getContainer()).create(name, Resource.CONTENT_TYPE_DIRECTORY) : (ManageableContainer<?>) ((RepositoryEntry) child).getContainer();
		EAINode node = child == null ? new EAINode() : ((RepositoryEntry) child).getNode();
		node.setArtifactManager(manager.getClass());
		node.setLeaf(child == null);
		if (child == null) {
			node.setReferences(new ArrayList<String>());
			node.setVersion(1);
			node.setLastModified(new Date());
			node.setEnvironmentId(InetAddress.getLocalHost().getHostName());
		}
		writeNode(nodeContainer, node);
		if (child == null) {
			child = new RepositoryEntry(getRepository(), nodeContainer, this, name);
			getChildren().put(name, child);
		}
		if (reload) {
			repository.reload(child.getId());
		}
//			// update the cached scan-typed list
//			if (repository instanceof EAIResourceRepository) {
//				((EAIResourceRepository) repository).scanForTypes(this);
//			}
		if (reload) {
			repository.getEventDispatcher().fire(new NodeEvent(getId() + "." + name, node, State.CREATE, true), this);
		}
		return (RepositoryEntry) child;
	}
	
	public void saveNode() {
		if (isNode()) {
			try {
				writeNode(getContainer(), getNode());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void writeNode(ResourceContainer<?> nodeContainer, EAINode node) throws IOException {
		Resource target = nodeContainer.getChild("node.xml");
		if (target == null) {
			target = ((ManageableContainer<?>) nodeContainer).create("node.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
		try {
			Marshaller marshaller = getJAXBContext().createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(node, IOUtils.toOutputStream(writable));
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
		// only fill in a created if there isn't one yet
		if (node.getCreated() == null) {
			node.setCreated(new Date());
		}
		node.setReferences(references == null ? new ArrayList<String>() : references);
		node.setVersion(node.getVersion() + 1);
		node.setLastModified(new Date());
		node.setEnvironmentId(InetAddress.getLocalHost().getHostName());
		writeNode(getContainer(), node);
		if (repository instanceof EAIResourceRepository) {
			((EAIResourceRepository) repository).updateReferences(getId(), references);
		}
	}
	
	@Override
	public void updateNodeContext(String environmentId, long version, Date lastModified) throws IOException {
		EAINode node = (EAINode) getNode();
		node.setEnvironmentId(environmentId);
		node.setVersion(version);
		node.setLastModified(lastModified);
		writeNode(getContainer(), node);
	}
	
	private CollectionImpl collection;
	private Date lastLoadedCollection;
	
	@Override
	public CollectionImpl getCollection() {
		Resource resource = container.getChild("collection.xml");
		if (resource != null && (collection == null || lastLoadedCollection == null || (resource instanceof TimestampedResource && ((TimestampedResource) resource).getLastModified().after(lastLoadedCollection)))) {
			synchronized(this) {
				if (resource != null && (collection == null || lastLoadedCollection == null || (resource instanceof TimestampedResource && ((TimestampedResource) resource).getLastModified().after(lastLoadedCollection)))) {
					try {
						ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
						try {
							collection = (CollectionImpl) getJAXBContext().createUnmarshaller().unmarshal(IOUtils.toInputStream(readable));
						}
						catch (JAXBException e) {
							throw new IOException(e);
						}
						finally {
							readable.close();
						}
					}
					catch (IOException e) {
						logger.error("Could not load collection " + getId(), e);
					}
		
					if (resource instanceof TimestampedResource) {
						lastLoadedCollection = ((TimestampedResource) resource).getLastModified();
					}
				}
			}
		}
		return collection;
	}
	
	public void setCollection(CollectionImpl collection) {
		try {
			writeCollection(getContainer(), collection);
			this.collection = collection;
			this.lastLoadedCollection = new Date();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void saveCollection() {
		CollectionImpl collection = getCollection();
		if (collection != null) {
			try {
				writeCollection(getContainer(), collection);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void writeCollection(ResourceContainer<?> nodeContainer, CollectionImpl node) throws IOException {
		Resource target = nodeContainer.getChild("collection.xml");
		if (target == null) {
			target = ((ManageableContainer<?>) nodeContainer).create("collection.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
		try {
			Marshaller marshaller = getJAXBContext().createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(node, IOUtils.toOutputStream(writable));
		}
		catch (JAXBException e) {
			throw new IOException(e);
		}
		finally {
			writable.close();
		}
	}
	
	public EAINode getNode() {
		// if we have a node, even if it is outdated, we want the system to force us to reload it
		// otherwise it leads to odd things because we tend to save (update node), then reload
		// but on reload, the node sees "hey, new node.xml!" fetches it and (more importantly) that tosses the already loaded artifact, then claim "nope, not loaded yet"
		if (node == null || reloadNode) {
			Resource resource = container.getChild("node.xml");
			if (resource != null && (node == null || lastLoaded == null || (resource instanceof TimestampedResource && ((TimestampedResource) resource).getLastModified().after(lastLoaded)))) {
				synchronized(this) {
					boolean isReload = false;
					try {
						if (resource != null && (node == null || lastLoaded == null || (resource instanceof TimestampedResource && ((TimestampedResource) resource).getLastModified().after(lastLoaded)))) {
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
									reloadNode = false;
								}
								catch (JAXBException e) {
									throw new IOException(e);
								}
								finally {
									readable.close();
								}
							}
							catch (IOException e) {
								logger.error("Could not " + (isReload ? "reload" : "load") + " node: " + getId(), e);
							}
				
							if (resource instanceof TimestampedResource) {
								lastLoaded = ((TimestampedResource) resource).getLastModified();
							}
							// trigger a reload _after_ we have updated the lastLoaded
							// otherwise we can get into a loop, the reload triggers a start in the server, the start (e.g. for a web application) does a getNode()
							// the getNode() sees the already loaded node but wants to reload it cause the timestamp is not clear, and triggers a reload etc etc
							repository.getEventDispatcher().fire(new NodeEvent(getId(), node, isReload ? State.RELOAD : State.LOAD, true), this);
						}
					}
					catch (Exception e) {
						logger.error("Could not " + (isReload ? "reload" : "load") + " node: " + getId(), e);
					}
				}
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
						jaxbContext = JAXBContext.newInstance(EAINode.class, CollectionImpl.class);
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

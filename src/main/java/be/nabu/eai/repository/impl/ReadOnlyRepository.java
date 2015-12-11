package be.nabu.eai.repository.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.events.RepositoryEvent;
import be.nabu.eai.repository.events.RepositoryEvent.RepositoryState;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceRunner;

public class ReadOnlyRepository implements ResourceRepository {

	private ResourceRepository local;
	private RepositoryEntry root;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	private Map<Class<? extends Artifact>, Map<String, Node>> nodesByType;
	private Map<String, List<String>> references = new HashMap<String, List<String>>(), dependencies = new HashMap<String, List<String>>();
	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean isLoading;
	
	public ReadOnlyRepository(ResourceRepository local, ResourceContainer<?> root) {
		this.local = local;
		this.root = new RepositoryEntry(this, root, null, root.getName());
	}
	
	@Override
	public Entry getEntry(String id) {
		return EAIRepositoryUtils.getEntry(getRoot(), id);
	}

	@Override
	public Charset getCharset() {
		return local.getCharset();
	}

	@Override
	public EventDispatcher getEventDispatcher() {
		return dispatcher;
	}

	@Override
	public Node getNode(String id) {
		return EAIRepositoryUtils.getNode(this, id);
	}

	@Override
	public void reload(String id) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void reloadAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unload(String id) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public List<Node> getNodes(Class<? extends Artifact> artifactClazz) {
		if (nodesByType == null) {
			scanForTypes();
		}
		List<Node> nodes = new ArrayList<Node>();
		for (Class<?> clazz : nodesByType.keySet()) {
			if (artifactClazz.isAssignableFrom(clazz)) {
				nodes.addAll(nodesByType.get(clazz).values());
			}
		}
		return nodes;
	}

	@Override
	public ServiceRunner getServiceRunner() {
		return local.getServiceRunner();
	}

	@Override
	public void setServiceRunner(ServiceRunner serviceRunner) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getReferences(String id) {
		return references.containsKey(id) ? new ArrayList<String>(references.get(id)) : new ArrayList<String>();
	}

	@Override
	public List<String> getDependencies(String id) {
		return dependencies.containsKey(id) ? new ArrayList<String>(dependencies.get(id)) : new ArrayList<String>();
	}

	@Override
	public void start() {
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.LOAD, false), this);
		isLoading = true;
		load(getRoot());
		isLoading = false;
		// IMPORTANT: this assumes the local server artifacts are in sync with the remote ones!! [IN-SYNC]
		// It is trivial to have multiple module versions in memory, it is however hard in the other repositories to know which one to use
		EAIResourceRepository.getInstance().reattachMavenArtifacts(root);
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.LOAD, true), this);
	}
	
	private void load(Entry entry) {
		logger.info("Loading: " + entry.getId());
		List<Entry> artifactRepositoryManagers = new ArrayList<Entry>();
		load(entry, artifactRepositoryManagers);
		// first load the repositories without dependencies
		for (Entry manager : artifactRepositoryManagers) {
			if (manager.getNode().getReferences() == null || manager.getNode().getReferences().isEmpty()) {
				loadArtifactManager(manager);
			}
		}
		// then the rest
		for (Entry manager : artifactRepositoryManagers) {
			if (manager.getNode().getReferences() != null && !manager.getNode().getReferences().isEmpty()) {
				loadArtifactManager(manager);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void loadArtifactManager(Entry entry) {
		logger.debug("Loading children of: " + entry.getId());
		try {
			List<Entry> addedChildren = ((ArtifactRepositoryManager) entry.getNode().getArtifactManager().newInstance()).addChildren((ModifiableEntry) entry, entry.getNode().getArtifact());
			if (addedChildren != null) {
				for (Entry addedChild : addedChildren) {
					buildReferenceMap(addedChild.getId(), addedChild.getNode().getReferences());
				}
			}
		}
		catch (Exception e) {
			logger.error("Could not finish loading generated children for: " + entry.getId(), e);
		}
	}
	
	private void buildReferenceMap(String id, List<String> references) {
		if (references != null) {
			this.references.put(id, references);
			for (String reference : references) {
				if (!dependencies.containsKey(reference)) {
					dependencies.put(reference, new ArrayList<String>());
				}
				dependencies.get(reference).add(id);
			}
		}
	}

	private void reset() {
		nodesByType = null;
	}
	
	private void load(Entry entry, List<Entry> artifactRepositoryManagers) {
		// don't refresh on initial load, this messes up performance for remote file systems
		if (!isLoading) {
			// refresh every entry before reloading it, there could be new elements (e.g. remote changes to repo)
			entry.refresh(false);
			// reset this to make sure any newly loaded entries are picked up or old entries are deleted
			reset();
		}
		if (entry.isNode()) {
			logger.info("Loading entry: " + entry.getId());
			buildReferenceMap(entry.getId(), entry.getNode().getReferences());
			if (entry instanceof ModifiableEntry && entry.isNode() && entry.getNode().getArtifactManager() != null && ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager())) {
				artifactRepositoryManagers.add(entry);
			}
		}
		if (!entry.isLeaf()) {
			for (Entry child : entry) {
				load(child, artifactRepositoryManagers);
			}
		}
	}
	
	@Override
	public ClassLoader newClassLoader(String artifact) {
		return local.newClassLoader(artifact);
	}

	@Override
	public <T> List<Class<T>> getImplementationsFor(Class<T> clazz) {
		return local.getImplementationsFor(clazz);
	}

	@Override
	public <T extends Artifact> List<T> getArtifacts(Class<T> artifactClazz) {
		return EAIRepositoryUtils.getArtifacts(this, artifactClazz);
	}

	@Override
	public Artifact resolve(String id) {
		return EAIRepositoryUtils.resolve(this, id);
	}

	@Override
	public ExecutionContext newExecutionContext(Principal principal) {
		return local.newExecutionContext(principal);
	}

	@Override
	public List<DefinedService> getServices() {
		List<Node> nodes = getNodes(DefinedService.class);
		List<DefinedService> services = new ArrayList<DefinedService>(nodes.size());
		for (Node node : nodes) {
			try {
				services.add((DefinedService) node.getArtifact());
			}
			catch (IOException e) {
				logger.error("Could not load " + node, e);
			}
			catch (ParseException e) {
				logger.error("Could not load " + node, e);
			}
		}
		return services;
	}

	@Override
	public ResourceEntry getRoot() {
		return root;
	}

	@Override
	public boolean isInternal(ResourceContainer<?> container) {
		return local.isInternal(container);
	}

	@Override
	public boolean isValidName(ResourceContainer<?> parent, String name) {
		return local.isValidName(parent, name);
	}

	private void scanForTypes() {
		if (nodesByType == null) {
			synchronized(this) {
				if (nodesByType == null) {
					nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
				}
			}
		}
		synchronized(nodesByType) {
			nodesByType.clear();
			scanForTypes(getRoot());
		}
	}
	
	private void scanForTypes(Entry entry) {
		if (nodesByType == null) {
			synchronized(this) {
				nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
			}
		}
		synchronized(nodesByType) {
			for (Entry child : entry) {
				if (child.isNode()) {
					Class<? extends Artifact> artifactClass = child.getNode().getArtifactClass();
					if (!nodesByType.containsKey(artifactClass)) {
						nodesByType.put(artifactClass, new HashMap<String, Node>());
					}
					nodesByType.get(artifactClass).put(child.getId(), child.getNode());
				}
				if (!child.isLeaf()) {
					scanForTypes(child);
				}
			}
		}
	}


}

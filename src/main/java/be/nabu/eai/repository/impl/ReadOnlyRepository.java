/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
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
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.metrics.api.MetricInstance;
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
	public ClassLoader getClassLoader() {
		return local.getClassLoader();
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends Artifact> List<T> getArtifacts(Class<T> artifactClazz) {
//		List<T> artifacts = new ArrayList<T>();
//		for (Node node : getNodes(artifactClazz)) {
//			try {
//				artifacts.add((T) node.getArtifact());
//			}
//			catch (Exception e) {
//				logger.error("Could not load node: " + node);
//			}
//		}
//		return artifacts;
//	}

	@Override
	public Artifact resolve(String id) {
		return EAIRepositoryUtils.resolve(this, id);
	}

	@Override
	public ExecutionContext newExecutionContext(Token primary, Token...alternatives) {
		return local.newExecutionContext(primary, alternatives);
	}

	@Override
	public List<DefinedService> getServices() {
		return getArtifacts(DefinedService.class);
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

	@Override
	public MetricInstance getMetricInstance(String id) {
		return null;
	}

	@Override
	public void reloadAll(Collection<String> ids) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getArtifacts(Class<T> ifaceClass) {
		List<T> results = new ArrayList<T>();
		if (nodesByType == null) {
			scanForTypes();
		}
		for (Class<?> clazz : nodesByType.keySet()) {
			if (ifaceClass.isAssignableFrom(clazz)) {
				for (Node node : nodesByType.get(clazz).values()) {
					try {
						Artifact artifact = node.getArtifact();
						if (artifact != null) {
							results.add((T) artifact);
						}
					}
					catch (Exception e) {
						logger.error("Could not load artifact", e);
					}
				}
			}
		}
		return results;
	}

	@Override
	public EventDispatcher getMetricsDispatcher() {
		return local.getMetricsDispatcher();
	}

	@Override
	public EventDispatcher getComplexEventDispatcher() {
		return local.getComplexEventDispatcher();
	}
}

package be.nabu.eai.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.MavenRepository;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.events.RepositoryEvent;
import be.nabu.eai.repository.events.RepositoryEvent.RepositoryState;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.resources.RepositoryResourceResolver;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.metrics.api.GroupLevelProvider;
import be.nabu.libs.metrics.core.MetricInstanceImpl;
import be.nabu.libs.metrics.core.api.Sink;
import be.nabu.libs.metrics.core.api.SinkProvider;
import be.nabu.libs.metrics.core.sinks.LimitedHistorySink;
import be.nabu.libs.metrics.impl.MetricGrouper;
import be.nabu.libs.metrics.impl.SystemMetrics;
import be.nabu.libs.resources.ResourceFactory;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.DefinedServiceInterfaceResolverFactory;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.ListableServiceContext;
import be.nabu.libs.services.SPIDefinedServiceInterfaceResolver;
import be.nabu.libs.services.ServiceRunnerFactory;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceAuthorizerProvider;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.services.pojo.POJOInterfaceResolver;
import be.nabu.libs.types.DefinedSimpleTypeResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.SPIDefinedTypeResolver;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * Ideally at some point I want to plug in the higher level VFS api instead of the low level resource API
 * To make this easier, I have tried to encapsulate as much resource-related stuff here as possible
 *
 * Interesting problem:
 * - for performance reasons and to prevent load-order issues, nodes are lazily loaded
 * - EXCEPT artifact repositories as they need to be loaded to recursively load whatever artifacts they additionally expose
 * - HOWEVER if one artifact repository has a dependency to another, they must load in the correct order
 * - in our case we had a JDBC adapter referencing a UML type, if the JDBC adapter is loaded() first (because its a repository), it will not find the as-of-yet-unloaded UML artifact repository
 * The solution is predicated on this assumption:
 * - node repositories should almost never have dependencies to one another as they are by definition objects created outside of the scope of nabu and unable to know that the other exists
 * - JDBC service is an exception as it is a hybrid: it is a native repository object but it also generates other objects for usage
 * The solution consists of two parts:
 * - first load all the nodes, then load all the artifact repositories
 * - in the artifact repository loading, we make a very rough distinction between those that have dependencies (very few of them) and those that don't, the ones without can be safely loaded
 * - if we add more hybrids like JDBC we will likely have to optimize this and perform actual dependency calculation, so for instance if a JDBC service requires "path.to.node" and only "path" exists which is a repository, load that one first
 * 
 * 
 * Classloading is tricky business. Consider this: we have maven artifacts that don't know each other by definition.
 * We have a repository that knows all maven artifacts.
 * The maven classloader still has the capability of depending on another module (a higher level dependency)
 * So the maven classloader has one parent classloader: the repository. Because it sees everything (native code + other maven classloaders)
 * But we don't want to recurse from maven classloader > repository class loader > maven classloader
 * For this reason we added a "non recursive" load for maven classloader
 * 
 * Note: in the future we should have "modules" which are zip files containing _everything_ for a module (jar files, flow services, sql,...). These "modules" are core in that they have to be loaded before everything else because they contain artifacts etc.
 * The second type will be simple "maven artifacts" created by the user which do not expose new module-level functionality and as such can be part of the normal boot (and deployment) process.
 * 
 * 
 * 
 * TODO: Factory pattern: a number of the factories are either shared (e.g. low level converters because code is considered in sync)
 * Or only a runtime issue (e.g. type conversion). Runtime is not a problem as actually running stuff is done on the remote server anyway (which has the correct repository)
 * The artifact classes (e.g. the actual MethodService etc) have to be loaded only once (otherwise we could do classloader trickery) otherwise it would break diff/merge
 * Any factory that resolves an artifact though (pretty much any factory explicitly updated by the repository) is affected by the context it runs in
 * For all these entry points (vm services, structures,...) I need to make sure we can set a context-aware factory instead of the generic getInstance()
 * This is a process that will take some time and is currently only required for diffing and deployment
 * But e.g. as long as there is no diff/merge of a vm service, we don't need to load it in a context-aware fashion
 * Currently the most important thing for diff/merge deploying is jaxbartifacts, so we will focus on those because they are easy to set
 * 
 */
public class EAIResourceRepository implements ResourceRepository, MavenRepository, SinkProvider, GroupLevelProvider {
	
	public static final String METRICS_SYSTEM = "system";
	
	private ResourceContainer<?> resourceRoot;
	private RepositoryEntry repositoryRoot;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	private List<be.nabu.libs.maven.ResourceRepository> mavenRepositories = new ArrayList<be.nabu.libs.maven.ResourceRepository>();
	private List<String> internalDomains;
	private List<ServiceRuntimeTrackerProvider> dynamicRuntimeTrackers = new ArrayList<ServiceRuntimeTrackerProvider>();
	
	private static EAIResourceRepository instance;
	
	private List<ArtifactManager<?>> artifactManagers;
	
	public static final String PRIVATE = "private";
	public static final String PUBLIC = "public";
	
	private boolean isLoading;
	
	private Map<Class<? extends Artifact>, Map<String, Node>> nodesByType;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static List<String> INTERNAL = Arrays.asList(new String [] { PRIVATE, PUBLIC });
	private static List<String> RESERVED = Arrays.asList(new String [] { PRIVATE, PUBLIC, "class", "package", "import", "for", "while", "if", "do", "else" });
	private Charset charset = Charset.forName("UTF-8");
	private ResourceContainer<?> mavenRoot;
	private URI localMavenServer;
	private boolean updateMavenSnapshots = false;
	
	private Map<String, List<String>> references = new HashMap<String, List<String>>(), dependencies = new HashMap<String, List<String>>();
	private List<MavenManager> mavenManagers = new ArrayList<MavenManager>();
	private List<MavenArtifact> mavenArtifacts = new ArrayList<MavenArtifact>();
	private MavenManager mavenManager;
	private Map<MavenArtifact, DefinedServiceInterfaceResolver> mavenIfaceResolvers = new HashMap<MavenArtifact, DefinedServiceInterfaceResolver>();
	private CacheProvider cacheProvider;

	private Map<String, MetricGrouper> metrics;
	
	public EAIResourceRepository() throws IOException, URISyntaxException {
		this(
			(ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/repository")), null),
			(ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/maven")), null)
		);
	}
	
	public EAIResourceRepository(ResourceContainer<?> repositoryRoot, ResourceContainer<?> mavenRoot) throws IOException {
		internalDomains = new ArrayList<String>();
		internalDomains.add("nabu");
		if (System.getProperty("repository.domains") != null) {
			internalDomains.addAll(Arrays.asList(System.getProperty("repository.domains").split("[\\s]*,[\\s]*")));
		}
		ContentTypeMap.register();
		this.resourceRoot = repositoryRoot;
		this.mavenRoot = mavenRoot;
		this.repositoryRoot = new RepositoryEntry(this, repositoryRoot, null, "/");
		// important for clusters
		ArtifactResolverFactory.getInstance().addResolver(this);
		// important for clusters
		DefinedTypeResolverFactory.getInstance().addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
		DefinedTypeResolverFactory.getInstance().addResolver(new EAIRepositoryTypeResolver(this));
		DefinedTypeResolverFactory.getInstance().addResolver(new SPIDefinedTypeResolver());
		// important for clusters
		DefinedServiceResolverFactory.getInstance().addResolver(new RepositoryServiceResolver(this));
		// service interface resolvers
		// this is important for clusters
		DefinedServiceInterfaceResolverFactory.getInstance().addResolver(new RepositoryServiceInterfaceResolver(this));
		DefinedServiceInterfaceResolverFactory.getInstance().addResolver(new SPIDefinedServiceInterfaceResolver());
		instance = this;
		// only important at runtime to resolve data
		// should not impact clusters
		ResourceFactory.getInstance().addResourceResolver(new RepositoryResourceResolver(this));
		// resolving of simple types
		// TODO: could include type registries?
		SimpleTypeWrapperFactory.getInstance().addWrapper(new RepositorySimpleTypeWrapper(this));
		this.cacheProvider = new EAIRepositoryCacheProvider(this);
	}
	
	/**
	 * This is mostly used by java services to find the repository
	 * As long as java services are limited to modules, this is acceptable as we don't stream modules from remote servers
	 * It however becomes slightly trickier once we have user-maven modules that are coming from remote servers...
	 * They would need access to their own (remote) repository...
	 */
	public static EAIResourceRepository getInstance() {
		return instance;
	}
	
	public ManageableContainer<?> getResourceContainer(String id) throws IOException {
		String path = id.replace('.', '/');
		Resource resource = ResourceUtils.resolve(resourceRoot, path);
		if (resource == null) {
			resource = ResourceUtils.mkdirs(resourceRoot, path);
		}
		return (ManageableContainer<?>) resource;
	}
	
	public ReadableContainer<ByteBuffer> getReadable(String id, String...paths) throws IOException {
		return new ResourceReadableContainer((ReadableResource) getResource(id, paths));
	}
	
	public WritableContainer<ByteBuffer> getWritable(String id, String...paths) throws IOException {
		return new ResourceWritableContainer((WritableResource) getResource(id, paths));
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
				Resource parentResource = ResourceUtils.resolve(resourceRoot, parent);
				if (parentResource == null) {
					parentResource = ResourceUtils.mkdirs(parentResource, parent);
				}
				resource = ((ManageableContainer<?>) parentResource).create(path, mimeType);
			}
		}
		return resource;
	}
	
	@Override
	public Node getNode(String id) {
		return EAIRepositoryUtils.getNode(this, id);
	}
	
	public void updateReferences(String id, List<String> references) {
		unbuildReferenceMap(id);
		buildReferenceMap(id, references);
	}
	
	private void buildReferenceMap(String id, List<String> references) {
		logger.debug("Loading references for '" + id + "': " + references);
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
	
	private void unbuildReferenceMap(String id) {
		this.references.remove(id);
		for (String dependency : dependencies.keySet()) {
			if (dependencies.get(dependency).contains(id)) {
				dependencies.get(dependency).remove(id);
			}
		}
	}
	
	@Override
	public List<String> getDependencies(String id) {
		return dependencies.containsKey(id) ? new ArrayList<String>(dependencies.get(id)) : new ArrayList<String>();
	}
	
	@Override
	public List<String> getReferences(String id) {
		return references.containsKey(id) ? new ArrayList<String>(references.get(id)) : new ArrayList<String>();
	}
	
	@Override
	public Charset getCharset() {
		return charset;
	}
	
	@Override
	public void unload(String id) {
		Entry entry = getEntry(id);
		if (entry != null) {
			unload(entry);
			entry.getParent().refresh(false);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void unload(Entry entry) {
		entry.refresh(false);
		logger.info("Unloading: " + entry.getId());
		reset();
		// TODO: don't actually remove the entry? perhaps use "modifiableentry" to remove the children from the parent? no issue so far though...
		// this is "partially" solved by the refresh we do in the parent unload(), because we usually unload in case of delete (then the refresh gets it) or update (in that case the load afterwards updates it)
		if (entry.getParent() != null) {
			entry.getParent().refresh(false);
		}
		if (entry.isNode()) {
			unbuildReferenceMap(entry.getId());
			// if there is an artifact manager and it maintains a repository, remove it all
			if (entry.getNode().isLoaded() && entry.getNode().getArtifactManager() != null && ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager())) {
				try {
					((ArtifactRepositoryManager) entry.getNode().getArtifactManager().newInstance()).removeChildren((ModifiableEntry) entry, entry.getNode().getArtifact());
				}
				catch (InstantiationException e) {
					logger.error("Could not finish unloading generated children for " + entry.getId(), e);
				}
				catch (IllegalAccessException e) {
					logger.error("Could not finish unloading generated children for " + entry.getId(), e);
				}
				catch (IOException e) {
					logger.error("Could not finish unloading generated children for " + entry.getId(), e);
				}
				catch (ParseException e) {
					logger.error("Could not finish unloading generated children for " + entry.getId(), e);
				}
			}
		}
		if (entry instanceof ResourceEntry) {
			Iterator<be.nabu.libs.maven.ResourceRepository> iterator = mavenRepositories.iterator();
			while (iterator.hasNext()) {
				be.nabu.libs.maven.ResourceRepository repository = iterator.next();
				if (((ResourceEntry) entry).getContainer().equals(repository.getRoot().getParent())) {
					// unload all maven artifacts related to this repository
					Iterator<MavenArtifact> artifactIterator = mavenArtifacts.iterator();
					while (artifactIterator.hasNext()) {
						MavenArtifact artifact = artifactIterator.next();
						if (artifact.getRepository().equals(repository)) {
							try {
								MavenManager.detachChildren(getRoot(), artifact);
							}
							catch (IOException e) {
								logger.error("Could not properly unload maven repository for: " + entry.getId(), e);
							}
							iterator.remove();
						}
					}
					// unload all the maven managers related to this repository
					Iterator<MavenManager> managerIterator = mavenManagers.iterator();
					while (managerIterator.hasNext()) {
						MavenManager mavenManager = managerIterator.next();
						if (mavenManager.getRepository().equals(repository)) {
							iterator.remove();
						}
					}
					// remove the repository itself
					iterator.remove();
				}
			}
		}
		if (!entry.isLeaf()) {
			for (Entry child : entry) {
				unload(child);
			}
		}
	}
	
	@Override
	public void reloadAll() {
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, false), this);
		unload(getRoot());
		references.clear();
		dependencies.clear();
		reset();
		load(getRoot());
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, true), this);
	}
	
	@Override
	public void reload(String id) {
		reload(id, true);
	}

	private void reloadAll(Collection<String> ids) {
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, false), this);
		Set<String> dependenciesToReload = new HashSet<String>();
		for (String id : ids) {
			Set<String> calculateDependenciesToReload = calculateDependenciesToReload(id);
			dependenciesToReload.removeAll(calculateDependenciesToReload);
			dependenciesToReload.addAll(calculateDependenciesToReload);
		}
		for (String id : dependenciesToReload) {
			reload(id, false);
		}
		reattachMavenArtifacts();
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, true), this);
	}
	
	private void reload(String id, boolean recursiveReload) {
		logger.info("Reloading: " + id);
		if (recursiveReload) {
			getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, false), this);
		}
		Entry entry = getEntry(id);
		// if we have an entry on the root which is not found, it could be new, reset the root (if possible) and try again
		// alternatively we can reload the entire root folder but this would have massive performance repercussions
		if (entry == null && !id.contains(".")) {
			getRoot().refresh(false);
			entry = getEntry(id);
		}
		// when we are reloading recursively, we are in the first reload iteration (not iterative)
		// if the artifact does not exist at this level, get a parent
		if (recursiveReload) {
			while (entry == null && id.contains(".")) {
				int index = id.lastIndexOf('.');
				id = id.substring(0, index);
				entry = getEntry(id);
				if (entry == null && !id.contains(".")) {
					getRoot().refresh(false);
					entry = getEntry(id);
				}
			}
		}
		if (entry != null) {
			unload(entry);
			load(entry);
			// also reload all the dependencies
			// prevent concurrent modification
			if (recursiveReload) {
				Set<String> dependenciesToReload = calculateDependenciesToReload(entry);
				for (String dependency : dependenciesToReload) {
					reload(dependency, false);
				}
			}
		}
		if (recursiveReload) {
			// TODO: remove
			reattachMavenArtifacts();
			getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, true), this);
		}
	}
	
	private Set<String> calculateDependenciesToReload(Entry entry) {
		Set<String> dependencies = new HashSet<String>();
		if (entry.isNode()) {
			dependencies.addAll(calculateDependenciesToReload(entry.getId()));
		}
		if (!entry.isLeaf()) {
			for (Entry child : entry) {
				Set<String> calculateDependenciesToReload = calculateDependenciesToReload(child);
				dependencies.removeAll(calculateDependenciesToReload);
				dependencies.addAll(calculateDependenciesToReload);
			}
		}
		return dependencies;
	}
	
	private Set<String> calculateDependenciesToReload(String id) {
		List<String> directDependencies = getDependencies(id);
		Set<String> dependenciesToReload = new LinkedHashSet<String>(directDependencies);
		for (String directDependency : directDependencies) {
			Set<String> indirectDependencies = calculateDependenciesToReload(directDependency);
			// remove any dependencies that are also in the indirect ones
			// we can add them again afterwards which means they will only be in the list once and in the correct order
			dependenciesToReload.removeAll(indirectDependencies);
			dependenciesToReload.addAll(indirectDependencies);
		}
		return dependenciesToReload;
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

	@SuppressWarnings({ "rawtypes" })
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
		if (entry instanceof ResourceEntry) {
			ResourceContainer<?> container = (ResourceContainer) ((ResourceEntry) entry).getContainer().getChild(PRIVATE);
			if (container != null) {
				boolean loadAsMaven = false;
				for (Resource resource : container) {
					if (resource.getName().endsWith(".jar")) {
						loadAsMaven = true;
						break;
					}
				}
				if (loadAsMaven) {
					try {
						startMavenRepository(container);
					}
					catch (IOException e) {
						logger.error("Could not load maven repository for: " + entry.getId(), e);
					}
				}
			}
		}
		if (!entry.isLeaf()) {
			for (Entry child : entry) {
				load(child, artifactRepositoryManagers);
			}
		}
	}

	@Override
	public Entry getEntry(String id) {
		return EAIRepositoryUtils.getEntry(getRoot(), id);
	}
	
	@Override
	public Artifact resolve(String id) {
		return EAIRepositoryUtils.resolve(this, id);
	}
	
	public List<String> rebuildReferences(String id, boolean recursive) {
		Entry entry = id == null ? getRoot() : getEntry(id);
		if (entry == null) {
			throw new IllegalArgumentException("Can not find node with id: " + id);
		}
		List<String> updatedArtifacts = new ArrayList<String>();
		rebuildReferences(entry, recursive, updatedArtifacts);
		return updatedArtifacts;
	}
	
	@SuppressWarnings("unchecked")
	private void rebuildReferences(Entry entry, boolean recursive, List<String> updatedArtifacts) {
		// some nodes (mostly auto-generated ones) don't have managers
		if (entry.isNode() && entry instanceof ModifiableEntry && entry.getNode().getArtifactManager() != null) {
			try {
				List<String> currentReferences = entry.getNode().getReferences();
				List<String> newReferences = entry.getNode().getArtifactManager().newInstance().getReferences(entry.getNode().getArtifact());
				if (newReferences == null) {
					newReferences = new ArrayList<String>();
				}
				if (!newReferences.equals(currentReferences)) {
					((ModifiableNodeEntry) entry).updateNode(newReferences);
					updatedArtifacts.add(entry.getId());
					// rebuild references for this node
					unbuildReferenceMap(entry.getId());
					buildReferenceMap(entry.getId(), newReferences);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (recursive && !entry.isLeaf()) {
			for (Entry child : entry) {
				rebuildReferences(child, recursive, updatedArtifacts);
			}
		}
	}

	public List<Validation<?>> move(String originalId, String newId, boolean delete) throws IOException {
		Entry sourceEntry = getEntry(originalId);
		Entry targetEntry = getEntry(newId);
		Entry targetParent = newId.contains(".") ? getEntry(newId.replaceAll("\\.[^.]+$", "")) : getRoot();
		String targetName = newId.contains(".") ? newId.replaceAll(".*\\.([^.]+)$", "$1") : newId;
		if (targetEntry != null) {
			throw new IOException("A node with the name '" + newId + "' already exists");
		}
		if (!(sourceEntry instanceof ResourceEntry)) {
			throw new IOException("The entry '" + originalId + "' is not a resource-based entry, it can not be moved");
		}
		if (targetParent == null) {
			throw new IOException("The parent for '" + newId + "' does not exist, can not move there");
		}
		if (!(targetParent instanceof ResourceEntry)) {
			throw new IOException("The parent entry '" + targetParent.getId() + "' is not resource-based");
		}
		if (!(((ResourceEntry) targetParent).getContainer() instanceof ManageableContainer)) {
			throw new IOException("The target is not manageable: " + targetParent.getId());
		}
		if (!(((ResourceEntry) sourceEntry).getContainer().getParent() instanceof ManageableContainer) && delete) {
			throw new IOException("The source parent is not manageable: " + sourceEntry.getId());
		}
		ResourceEntry parent = (ResourceEntry) targetParent;
		if (parent.getContainer().getChild(targetName) != null || !isValidName(parent.getContainer(), targetName)) {
			throw new IOException("The name is not valid: " + targetName);
		}
		ResourceEntry entry = (ResourceEntry) sourceEntry;
		// make sure we have the latest view on the system
		entry.refresh(true);
		// copy the contents to the new location
		ResourceUtils.copy(entry.getContainer(), (ManageableContainer<?>) parent.getContainer(), targetName);
		// we need to refresh the parent entry as it can cache the children and not see the new addition
		targetParent.refresh(true);
		// load the new node
		Entry newEntry = getEntry(newId);
		if (newEntry == null) {
			throw new IOException("Could not load new entry: " + newId);
		}
		load(newEntry);
		List<Validation<?>> validations = new ArrayList<Validation<?>>();
		// remove the contents from the old location if necessary
		if (delete) {
			// move the dependencies
			relink(entry, newEntry, validations, true);
			// unload the node
			unload(entry.getId());
			// delete the original contents
			((ManageableContainer<?>) entry.getContainer().getParent()).delete(entry.getName());
			// refresh the parent so it picks up the file system change
			if (entry.getParent() != null) {
				entry.getParent().refresh(false);
			}
		}
		scanForTypes();
		return validations;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Set<String> relink(Entry from, Entry to, List<Validation<?>> validations, boolean reload) {
		logger.debug("Relinking: " + from.getId() + " to " + to.getId());
		Set<String> dependencies = new HashSet<String>(getDependencies(from.getId()));
		if (dependencies != null) {
			for (String dependency : dependencies) {
				logger.debug("Relinking dependency: " + dependency);
				Entry dependencyEntry = getEntry(dependency);
				if (dependencyEntry instanceof ResourceEntry) {
					Node node = dependencyEntry.getNode();
					if (node != null) {
						try {
							ArtifactManager artifactManager = node.getArtifactManager().newInstance();
							// update the references
							validations.addAll(artifactManager.updateReference(node.getArtifact(), from.getId(), to.getId()));
							// save the updated references
							artifactManager.save((ResourceEntry) dependencyEntry, node.getArtifact());
							// reload the new artifact
							reload(dependency, false);
						}
						catch (Exception e) {
							logger.error("Could not update reference for dependency '" + dependency + "' from '" + from.getId() + "' to '" + to.getId() + "'", e);
						}
					}
				}
				else {
					validations.add(new ValidationMessage(Severity.ERROR, "Can not update dependency '" + dependency + "' as it is not a resource entry"));
				}
			}
		}
		// recurse!
		if (!from.isLeaf()) {
			for (Entry child : from) {
				Entry target = to.getChild(child.getName());
				if (target == null) {
					validations.add(new ValidationMessage(Severity.ERROR, "Can not find moved copy of " + child.getId() + " in " + to.getId()));
				}
				else {
					dependencies.addAll(relink(child, target, validations, false));
				}
			}
		}
		if (reload) {
			reloadAll(dependencies);
		}
		return dependencies;
	}
	
	public String getId(ResourceContainer<?> container) {
		String id = "";
		while (!container.equals(resourceRoot)) {
			if (id.isEmpty()) {
				id = container.getName();
			}
			else {
				id = container.getName() + "." + id;
			}
			container = container.getParent();
		}
		if (!container.equals(resourceRoot)) {
			throw new IllegalArgumentException("The container is not part of the repository");
		}
		else if (id.isEmpty()) {
			throw new IllegalArgumentException("The container is invalid");
		}
		return id;
	}

	@Override
	public EventDispatcher getEventDispatcher() {
		return dispatcher;
	}

	@Override
	public RepositoryEntry getRoot() {
		return repositoryRoot;
	}

	@Override
	public boolean isInternal(ResourceContainer<?> container) {
		return INTERNAL.contains(container.getName());
	}

	@Override
	public boolean isValidName(ResourceContainer<?> parent, String name) {
		return name.matches("^[a-z]+[\\w]+$") && !RESERVED.contains(name);
	}
	
	@Override
	public void unloadMavenArtifact(be.nabu.libs.maven.api.Artifact artifact) {
		Iterator<MavenArtifact> iterator = mavenArtifacts.iterator();
		while(iterator.hasNext()) {
			MavenArtifact mavenArtifact = iterator.next();
			if (mavenIfaceResolvers.containsKey(mavenArtifact)) {
				DefinedServiceInterfaceResolverFactory.getInstance().removeResolver(mavenIfaceResolvers.get(mavenArtifact));
				mavenIfaceResolvers.remove(mavenArtifact);
			}
			// only unload artifacts from the ROOT repository, the other repositories are deemed unmodifiable
			if (mavenArtifact.getRepository().equals(mavenManager.getRepository())) {
				// if it is the same artifact (version doesn't matter), reload it
				if (mavenArtifact.getArtifact().getGroupId().equals(artifact.getGroupId()) && mavenArtifact.getArtifact().getArtifactId().equals(artifact.getArtifactId())) {
					try {
						mavenManager.removeChildren(getRoot(), mavenArtifact);
					}
					catch (IOException e) {
						logger.error("Could not remove children of maven artifact: " + artifact, e);
					}
					iterator.remove();
				}
			}
		}
	}
	
	@Override
	public void loadMavenArtifact(be.nabu.libs.maven.api.Artifact artifact) {
		if (mavenManager.getRepository().isInternal(artifact)) {
			startMavenArtifact(mavenManager, artifact, false);
		}
	}
	
	private void startMavenRepository(ResourceContainer<?> target) throws IOException {
		logger.info("Starting maven repository located at: " + ResourceUtils.getURI(target));
		be.nabu.libs.maven.ResourceRepository mavenRepository = new be.nabu.libs.maven.ResourceRepository(target, getEventDispatcher());
		mavenRepository.getDomains().addAll(internalDomains);
		mavenRepository.scan(false);
		mavenRepositories.add(mavenRepository);
		startMavenRepository(mavenRepository);
	}

	private void startMavenRepository(be.nabu.libs.maven.ResourceRepository mavenRepository) {
		// the first maven manager is considered the root manager and the only that has modifiable artifacts
		MavenManager mavenManager = new MavenManager(mavenRepository, DefinedTypeResolverFactory.getInstance().getResolver());
		if (this.mavenManager == null) {
			this.mavenManager = mavenManager;
		}
		mavenManagers.add(mavenManager);
		try {
			// do an initial load of all internal artifacts
			for (be.nabu.libs.maven.api.Artifact internal : mavenRepository.getInternalArtifacts()) {
				startMavenArtifact(mavenManager, internal, true);
			}
		}
		catch (IOException e) {
			logger.error("Could not load artifacts from maven repository", e);
		}
	}

	private void startMavenArtifact(MavenManager mavenManager, be.nabu.libs.maven.api.Artifact internal, boolean initial) {
		try {
			logger.info("Loading maven artifact " + internal.getGroupId() + " > " + internal.getArtifactId());
			MavenArtifact artifact = mavenManager.load(this, internal, localMavenServer, updateMavenSnapshots);
			mavenArtifacts.add(artifact);
			// there are two stages in the loading of a maven artifact:
			// - create a classloader capable of looking for classes/resources
			// - scan the package to check for any classes (services or beans) that have to be exposed
			// The first step is triggered by creating the artifact (in the few lines above this)
			// The second step is triggered by adding the children of the artifact to the tree
			// The "problem" with the second step is that some maven artifacts are interdependent on other artifacts and there is no "trivial" way to find out (we'd have to scan the poms and determine dependencies that way)
			// Due to the dependencies it is key that the modules are scanned in the correct order because triggering a "load" on a class could trigger the resolution of the dependent class
			// To (quickly) circumvent this issue, we simply don't attach the children on startup. In the startup sequence there was already a "reattach" at the very end because other loading can reset the initial attach anyway
			// This is still a rather dirty hack and it would be better to have the dependency checking or really split up the two stages cleanly
			if (!initial) {
				mavenManager.removeChildren(getRoot(), artifact);
				mavenManager.addChildren(getRoot(), artifact);
			}
			mavenIfaceResolvers.put(artifact, new POJOInterfaceResolver(artifact.getClassLoader()));
			DefinedServiceInterfaceResolverFactory.getInstance().addResolver(mavenIfaceResolvers.get(artifact));
		}
		catch (IOException e) {
			logger.error("Could not load artifact: " + internal.getGroupId() + " > " + internal.getArtifactId(), e);
		}
	}
	
	private void reattachMavenArtifacts() {
		reattachMavenArtifacts(getRoot());
	}

	public void reattachMavenArtifacts(ModifiableEntry entry) {
		for (MavenArtifact artifact : mavenArtifacts) {
			try {
				MavenManager.attachChildren(entry, artifact);
			}
			catch (IOException e) {
				logger.error("Could not reattach maven artifact: " + artifact, e);
			}
		}
	}
	
	public be.nabu.libs.maven.api.DomainRepository getMavenRepository() {
		return mavenManager != null ? mavenManager.getRepository() : null;
	}
	
	public ListableServiceContext getServiceContext() {
		return new ListableServiceContext() {
			private EAIRepositoryServiceTrackerProvider trackerProvider = new EAIRepositoryServiceTrackerProvider(EAIResourceRepository.this);
			private EAIRepositoryServiceAuthorizerProvider authorizerProvider = new EAIRepositoryServiceAuthorizerProvider(EAIResourceRepository.this);
			@SuppressWarnings("unchecked")
			@Override
			public <T extends Artifact> ArtifactResolver<T> getResolver(Class<T> arg0) {
				return (ArtifactResolver<T>) EAIResourceRepository.this;
			}
			@Override
			public <T extends Artifact> Collection<T> getArtifacts(Class<T> artifactType) {
				return EAIResourceRepository.this.getArtifacts(artifactType);
			}
			@Override
			public CacheProvider getCacheProvider() {
				return cacheProvider;
			}
			@Override
			public ServiceRuntimeTrackerProvider getServiceTrackerProvider() {
				return trackerProvider;
			}
			@Override
			public ServiceAuthorizerProvider getServiceAuthorizerProvider() {
				return authorizerProvider;
			}
		};
	}
	
	public CacheProvider getCacheProvider() {
		return cacheProvider;
	}

	@Override
	public ExecutionContext newExecutionContext(Principal principal) {
		return new EAIExecutionContext(this, principal, isDevelopment());
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getArtifactsThatImplement(Class<T> ifaceClass) {
		List<T> results = new ArrayList<T>();
		if (nodesByType == null) {
			scanForTypes();
		}
		for (Class<?> clazz : nodesByType.keySet()) {
			if (ifaceClass.isAssignableFrom(clazz)) {
				for (Node node : nodesByType.get(clazz).values()) {
					try {
						results.add((T) node.getArtifact());
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
	public <T extends Artifact> List<T> getArtifacts(Class<T> artifactClazz) {
		return EAIRepositoryUtils.getArtifacts(this, artifactClazz);
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
	
	public void scanForTypes() {
		if (nodesByType == null) {
			synchronized(this) {
				if (nodesByType == null) {
					nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
				}
			}
		}
		synchronized(nodesByType) {
			nodesByType.clear();
			scanForTypes(repositoryRoot);
		}
	}
	
	public void scanForTypes(Entry entry) {
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
	public ServiceRunner getServiceRunner() {
		return ServiceRunnerFactory.getInstance().getServiceRunner();
	}

	@Override
	public void setServiceRunner(ServiceRunner serviceRunner) {
		ServiceRunnerFactory.getInstance().setServiceRunner(serviceRunner);
	}

	@Override
	public void start() {
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.LOAD, false), this);
		// start the maven repository stuff
		try {
			startMavenRepository(mavenRoot);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		// before we start loading everything else, first do an initial attach so all the artifacts are loaded (for dependencies)
		reattachMavenArtifacts();
		isLoading = true;
		load(repositoryRoot);
		isLoading = false;
		// the load can remove maven artifacts, so attach again
		reattachMavenArtifacts();
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.LOAD, true), this);
	}

	@Override
	public URI getMavenRoot() {
		return ResourceUtils.getURI(mavenRoot);
	}

	public URI getLocalMavenServer() {
		return localMavenServer;
	}

	public void setLocalMavenServer(URI localMavenServer) {
		this.localMavenServer = localMavenServer;
	}

	public boolean isUpdateMavenSnapshots() {
		return updateMavenSnapshots;
	}

	public void setUpdateMavenSnapshots(boolean updateMavenSnapshots) {
		this.updateMavenSnapshots = updateMavenSnapshots;
	}
	
	public static boolean isDevelopment() {
		return Boolean.TRUE.equals(Boolean.parseBoolean(System.getProperty("development", "false")));
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T> List<Class<T>> getImplementationsFor(Class<T> clazz) {
		List<Class<T>> implementations = new ArrayList<Class<T>>();
		for (MavenArtifact artifact : mavenArtifacts) {
			try {
				List provided = artifact.getImplementations().get(clazz);
				if (provided != null) {
					implementations.addAll(provided);
				}
			}
			catch (IOException e) {
				logger.error("Could not search artifact for implementations", e);
			}
		}
		return implementations;
	}
	
	@Override
	public ClassLoader newClassLoader(String artifact) {
		return new RepositoryClassLoader(this, getClass().getClassLoader(), artifact);
	}
	
	public Class<?> loadClass(String className) {
		return loadClass(className, new ArrayList<String>());
	}
	
	public Class<?> loadClass(String className, List<String> blacklist) {
		for (MavenArtifact artifact : mavenArtifacts) {
			if (!blacklist.contains(artifact.getId())) {
				try {
					Class<?> clazz = artifact.getClassLoader().loadClassNonRecursively(className);
					if (clazz != null) {
						return clazz;
					}
				}
				catch (ClassNotFoundException e) {
					// ignore
				}
			}
		}
		return null;
	}

	public InputStream getMavenResource(String name) {
		for (MavenArtifact artifact : mavenArtifacts) {
			InputStream stream = artifact.getClassLoader().getResourceAsStream(name);
			if (stream != null) {
				return stream;
			}
		}
		return null;
	}
	
	private void reset() {
		nodesByType = null;
	}
	
	public Map<Class<? extends Artifact>, Map<String, Node>> getManageableNodes() {
		if (nodesByType == null) {
			scanForTypes();
		}
		Map<Class<? extends Artifact>, Map<String, Node>> manageableNodes = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
		for (Class<? extends Artifact> clazz : nodesByType.keySet()) {
			if (StartableArtifact.class.isAssignableFrom(clazz) && StoppableArtifact.class.isAssignableFrom(clazz)) {
				manageableNodes.put(clazz, nodesByType.get(clazz));
			}
		}
		return manageableNodes;
	}

	public List<ServiceRuntimeTrackerProvider> getDynamicRuntimeTrackers() {
		return dynamicRuntimeTrackers;
	}

	@Override
	public MetricGrouper getMetricInstance(String id) {
		if (metrics != null) {
			if (!metrics.containsKey(id)) {
				synchronized(metrics) {
					if (!metrics.containsKey(id)) {
						metrics.put(id, new MetricGrouper(new MetricInstanceImpl(id, this), this));
					}
				}
			}
			return metrics.get(id);
		}
		return null;
	}
	
	private Properties metricsProperties;
	
	public int getMetricsLevel(String name) {
		if (metricsProperties == null) {
			synchronized(this) {
				if (metricsProperties == null) {
					Properties properties = new Properties();
					InputStream input = getClass().getClassLoader().getResourceAsStream("metrics.properties");
					if (input != null) {
						try {
							try {
								properties.load(input);
							}
							finally {
								input.close();
							}
						}
						catch (IOException e) {
							logger.error("Could not load metrics file", e);
						}
					}
					this.metricsProperties = properties;
				}
			}
		}
		return Integer.parseInt(metricsProperties.getProperty(name, "1"));
	}
	
	public void enableMetrics(boolean enable) {
		if (enable && metrics == null) {
			synchronized(this) {
				if (metrics == null) {
					metrics = new HashMap<String, MetricGrouper>();
					synchronized(metrics) {
						MetricGrouper metric = new MetricGrouper(new MetricInstanceImpl(METRICS_SYSTEM, this), this);
						metrics.put(METRICS_SYSTEM, metric);
						SystemMetrics.record(metric);
					}
				}
			}
		}
		else if (!enable) {
			metrics = null;
		}
	}
	
	public Collection<String> getMetricInstances() {
		return metrics == null ? null : metrics.keySet();
	}

	@Override
	public Sink newSink(String id, String category) {
		return new LimitedHistorySink(1000);
	}

	@Override
	public Integer getLevel(String group) {
		return getMetricsLevel(group);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T extends Artifact> ArtifactManager<T> getArtifactManager(Class<T> artifactClass) {
		if (artifactManagers == null) {
			synchronized (this) {
				if (artifactManagers == null) {
					List<ArtifactManager<?>> managers = new ArrayList<ArtifactManager<?>>();
					for (ArtifactManager<?> manager : ServiceLoader.load(ArtifactManager.class)) {
						managers.add(manager);
					}
					for (Class<ArtifactManager> manager : getImplementationsFor(ArtifactManager.class)) {
						try {
							managers.add(manager.newInstance());
						}
						catch (Exception e) {
							logger.error("Could not load manager: " + manager, e);
						}
					}
					this.artifactManagers = managers;
				}
			}
		}
		ArtifactManager<T> closest = null;
		for (ArtifactManager<?> manager : artifactManagers) {
			if (manager.getArtifactClass().isAssignableFrom(artifactClass)) {
				if (closest == null || closest.getArtifactClass().isAssignableFrom(manager.getArtifactClass())) {
					closest = (ArtifactManager<T>) manager;
				}
			}
		}
		return closest;
	}

}

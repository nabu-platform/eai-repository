package be.nabu.eai.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.DynamicEntry;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.eai.repository.api.FeatureConfigurator;
import be.nabu.eai.repository.api.LicenseManager;
import be.nabu.eai.repository.api.LicensedRepository;
import be.nabu.eai.repository.api.MavenRepository;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.api.Templater;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager;
import be.nabu.eai.repository.events.NodeEvent;
import be.nabu.eai.repository.events.RepositoryEvent;
import be.nabu.eai.repository.events.NodeEvent.State;
import be.nabu.eai.repository.events.RepositoryEvent.RepositoryState;
import be.nabu.eai.repository.impl.CorrelationIdEnricher;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.resources.RepositoryResourceResolver;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.LocalClassLoader;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.artifacts.api.ClassProvidingArtifact;
import be.nabu.libs.artifacts.api.LazyArtifact;
import be.nabu.libs.artifacts.api.LiveReloadable;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.jaas.JAASConfiguration;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.maven.api.DomainRepository;
import be.nabu.libs.metrics.core.GaugeHistorizer;
import be.nabu.libs.metrics.core.MetricInstanceImpl;
import be.nabu.libs.metrics.core.api.ListableSinkProvider;
import be.nabu.libs.metrics.core.api.Sink;
import be.nabu.libs.metrics.core.sinks.LimitedHistorySinkWithStatistics;
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
import be.nabu.libs.resources.internal.MultipleURLStreamHandlerFactory;
import be.nabu.libs.resources.internal.VFSURLStreamHandlerFactory;
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
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.java.DomainObjectFactory;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.IOUtils;
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
public class EAIResourceRepository implements ResourceRepository, MavenRepository, ListableSinkProvider, LicensedRepository {
	
	public static final String METRICS_SYSTEM = "system";
	
	private ResourceContainer<?> resourceRoot;
	private RepositoryEntry repositoryRoot;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	private EventDispatcher complexDispatcher;
	private List<DomainRepository> mavenRepositories = new ArrayList<DomainRepository>();
	private List<String> internalDomains;
	private List<ServiceRuntimeTrackerProvider> dynamicRuntimeTrackers = new ArrayList<ServiceRuntimeTrackerProvider>();
	private String name;
	private String group;
	private RoleHandler roleHandler;
	private PermissionHandler permissionHandler;
	private LicenseManager licenseManager;
	private List<String> aliases = new ArrayList<String>();
	private List<String> enableEvents = Arrays.asList(System.getProperty("metric.events", METRICS_SYSTEM).split("[\\s]*,[\\s]*"));
	private static Boolean allowFileBasedRefactor = Boolean.parseBoolean(System.getProperty("file.refactor", "true"));
	
	private static Boolean allowLiveReload = Boolean.parseBoolean(System.getProperty("live.reload", "" + isDevelopment()));
	
	private static EAIResourceRepository instance;
	
	private EventDispatcher metricsDispatcher;
	private GaugeHistorizer metricsGaugeHistorizer;
	private boolean historizeGauges;
	
	public static final String PRIVATE = "private";
	public static final String PUBLIC = "public";
	public static final String PROTECTED = "protected";
	
	private Map<String, Map<String, List<Validation<?>>>> messages = new HashMap<String, Map<String, List<Validation<?>>>>();
	
	private boolean isLoading;
	
	private Map<Class<? extends Artifact>, Map<String, Node>> nodesByType;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private List<Templater> templaters = new ArrayList<Templater>();
	
	private static List<String> INTERNAL = Arrays.asList(new String [] { PRIVATE, PUBLIC, PROTECTED });
	public static List<String> RESERVED = Arrays.asList(new String [] { PRIVATE, PUBLIC, PROTECTED, "abstract", "assert", 
			"boolean", "integer", "short", "int", "double", "float", "char", "byte", 
			"case", "catch", "class", "const", "continue", "default", "do", "else", "enum", "extends", "final", "finally", 
			"for", "goto", "if", "implements", "import", "instanceof", "interface", "native", "new", "package", 
			"return", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while", 
			"META-INF" });
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

	private List<ClassProvidingArtifact> classProvidingArtifacts = new ArrayList<ClassProvidingArtifact>();
	
	private Map<String, MetricGrouper> metrics;

	private EAIRepositoryClassLoader classLoader;
	
	private Map<String, LimitedHistorySinkWithStatistics> sinks = new HashMap<String, LimitedHistorySinkWithStatistics>();
	
	private long historizationInterval = 5000;
	private int historySize = 500;
	private Map<String, EventEnricher> eventEnrichers = new HashMap<String, EventEnricher>();

	public EAIResourceRepository() throws IOException, URISyntaxException {
		this(
			(ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/repository")), null),
			(ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/maven")), null)
		);
	}
	
	public EAIResourceRepository(ResourceContainer<?> repositoryRoot, ResourceContainer<?> mavenRoot) throws IOException {
		// meant for complex event dispatching
		// we want to do the event enriching _synchronously_, allowing you to use thread locals etc to communicate if needed
		complexDispatcher = new EventDispatcherImpl(Runtime.getRuntime().availableProcessors()) {
			@SuppressWarnings("unchecked")
			@Override
			public <E> void fire(E event, Object source) {
				if (event != null && !eventEnrichers.isEmpty()) {
					for (EventEnricher enricher : new ArrayList<EventEnricher>(eventEnrichers.values())) {
						try {
							Object enriched = enricher.enrich(event);
							if (enriched != null) {
								event = (E) enriched;
							}
						}
						catch (Exception e) {
							logger.error("Failed to enrich event", e);
						}
					}
				}
				super.fire(event, source);
			}
		};
		
		internalDomains = new ArrayList<String>();
		internalDomains.add("nabu");
		if (System.getProperty("repository.domains") != null) {
			internalDomains.addAll(Arrays.asList(System.getProperty("repository.domains").split("[\\s]*,[\\s]*")));
		}
		// register custom content type mapping
		ContentTypeMap.register();
		// register custom URL handler that supports VFS lookups and local classloader lookups
		MultipleURLStreamHandlerFactory.register(Arrays.asList(
			new VFSURLStreamHandlerFactory(),
			new LocalClassLoader.LocalClassLoaderURLStreamHandlerFactory()
		));
		this.resourceRoot = repositoryRoot;
		this.mavenRoot = mavenRoot;
		this.repositoryRoot = new RepositoryEntry(this, repositoryRoot, null, "/");
		this.classLoader = new EAIRepositoryClassLoader(this);
		// important for clusters
		ArtifactResolverFactory.getInstance().addResolver(this);
		// important for clusters
		DefinedTypeResolverFactory.getInstance().addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
		DefinedTypeResolverFactory.getInstance().addResolver(new RepositoryTypeResolver(this));
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
		BeanResolver.getInstance().addFactory(new DomainObjectFactory() {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				return EAIResourceRepository.this.getClassLoader().loadClass(name);
			}
		});
		// register the central jaas configuration
		JAASConfiguration.register();
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
					parentResource = ResourceUtils.mkdirs(resourceRoot, parent);
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
		Entry entry = getEntry(id);
		// @2020-09-29
		// if it is not a resource entry, it is probably a dynamic one
		// let's deduce the parent and add it as a reference to make sure the reloading is done correctly (reloading uses the reference/dependency map)
		if (entry != null && entry instanceof DynamicEntry) {
			String originatingArtifact = ((DynamicEntry) entry).getOriginatingArtifact();
			if (references == null) {
				references = new ArrayList<String>();
			}
			if (!references.contains(originatingArtifact)) {
				references.add(originatingArtifact);
			}
		}
		else if (entry == null) {
			logger.warn("Could not find entry to build reference map for: " + id);
		}
		if (references != null) {
			logger.debug("Loading references for '" + id + "': " + references);
			this.references.put(id, new ArrayList<String>(references));
			for (String reference : references) {
				if (!dependencies.containsKey(reference)) {
					dependencies.put(reference, new ArrayList<String>());
				}
				dependencies.get(reference).add(id);
			}
			Node node = getNode(id);
			for (ClassProvidingArtifact provider : classProvidingArtifacts) {
				try {
					if (provider.loadClass(node.getArtifactManager().getName()) != null) {
						if (!this.references.get(id).contains(provider.getId())) {
							this.references.get(id).add(provider.getId());
						}
						if (!dependencies.containsKey(provider.getId())) {
							dependencies.put(provider.getId(), new ArrayList<String>());
						}
						if (!dependencies.get(provider.getId()).contains(id)){ 
							dependencies.get(provider.getId()).add(id);
						}
					}
				}
				catch (Exception e) {
					// ignore
				}
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
			getEventDispatcher().fire(new RepositoryEvent(RepositoryState.UNLOAD, false), this);
			unload(entry, true);
			if (entry.getParent() != null) {
				entry.getParent().refresh(false);
			}
			getEventDispatcher().fire(new RepositoryEvent(RepositoryState.UNLOAD, true), this);
		}
	}
	
	/**
	 * Known issue: when you unload a folder, you do an entry.refresh(false) on the folder
	 * When you have redeployed an entire application, that means it will reload the artifacts within
	 * Which means those artifacts come into a state where "isLoaded()" is false 
	 * Which means if those artifacts are/were artifact repository managers, they do not get to unload their previously loaded children
	 * If however we skip the check "isLoaded", we can have other problems with dependencies between dynamically loaded artifacts (e.g. jdbc depends on uml type)
	 * Currently on load, the nodes should be overwritten preventing any issues so we leave it as such....for now...
	 * 
	 * Additionally: suppose as developer you remove a folder which has subfolders and subnodes etc
	 * We first unload the root folder and only then proceed to unload the children. However the files have already been deleted.
	 * This currently works presumably due to the filesystem caching and only very specific resetting.
	 * However this is a fragile system.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void unload(Entry entry, boolean trigger) {
		if (entry.isNode()) {
			Node node = entry.getNode();
			if (node != null) {
				getEventDispatcher().fire(new NodeEvent(entry.getId(), entry.getNode(), State.UNLOAD, false), this);
				// remove it from the classloading (if applicable)
				if (node.isLoaded() && ClassProvidingArtifact.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
					try {
						classProvidingArtifacts.remove(entry.getNode().getArtifact());
					}
					catch (Exception e) {
						logger.error("Could not remove the entry from the classloading: " + entry.getId(), e);
					}
				}
				unbuildReferenceMap(entry.getId());
				// if there is an artifact manager and it maintains a repository, remove it all
				if (entry.getNode().isLoaded() && entry.getNode().getArtifactManager() != null && ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager())) {
					try {
						List<Entry> removedChildren = ((ArtifactRepositoryManager) entry.getNode().getArtifactManager().newInstance()).removeChildren((ModifiableEntry) entry, entry.getNode().getArtifact());
						logger.info("Unloaded " + (removedChildren == null ? "no" : removedChildren.size()) + " dynamic children of artifact: " + entry.getId());
						if (removedChildren != null) {
							for (Entry removedChild : removedChildren) {
								if (removedChild != null) {
									unbuildReferenceMap(removedChild.getId());
								}
							}
						}
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
				getEventDispatcher().fire(new NodeEvent(entry.getId(), entry.getNode(), State.UNLOAD, true), this);
			}
		}
		entry.refresh(false);
		logger.info("Unloading: " + entry.getId());
		reset();
		// TODO: don't actually remove the entry? perhaps use "modifiableentry" to remove the children from the parent? no issue so far though...
		// this is "partially" solved by the refresh we do in the parent unload(), because we usually unload in case of delete (then the refresh gets it) or update (in that case the load afterwards updates it)
		if (entry.getParent() != null) {
			entry.getParent().refresh(false);
		}
		// recurse to children
		for (Entry child : entry) {
			unload(child, trigger);
		}
	}
	
	@Override
	public void reloadAll() {
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, false), this);
		unload(getRoot(), false);
		references.clear();
		dependencies.clear();
		reset();
		preload(getRoot());
		load(getRoot());
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, true), this);
	}
	
	@Override
	public void reload(String id) {
		reload(id, true);
	}

	@Override
	public void reloadAll(Collection<String> ids) {
		reloadAll(ids, false);
	}
	
	private void reloadAll(Collection<String> ids, boolean dependenciesOnly) {
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, false), this);
		Set<String> dependenciesToReload = new HashSet<String>();
		for (String id : ids) {
			Set<String> calculateDependenciesToReload = calculateDependenciesToReload(id);
			dependenciesToReload.removeAll(calculateDependenciesToReload);
			dependenciesToReload.addAll(calculateDependenciesToReload);
		}
		if (dependenciesOnly) {
			for (String id : dependenciesToReload) {
				reload(id, false);
			}
		}
		else {
			ArrayList<String> cores = new ArrayList<String>(ids);
			cores.removeAll(dependenciesToReload);
			cores.addAll(dependenciesToReload);
			for (String id : cores) {
				reload(id, false);
			}
		}
		reattachMavenArtifacts();
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, true), this);
	}

	
	private void reload(String id, boolean recursiveReload) {
		logger.info("Reloading: " + id + " (" + recursiveReload + ")");
		if (recursiveReload) {
			getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, false), this);
		}
		Entry entry = id == null ? getRoot() : getEntry(id);
		// if we have an entry on the root which is not found, it could be new, reset the root (if possible) and try again
		// alternatively we can reload the entire root folder but this would have massive performance repercussions
		if (entry == null && !id.contains(".")) {
			getRoot().refresh(true, false);
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
			boolean liveReload = false;
			if (canLiveReload(entry)) {
				try {
					liveReload = ((LiveReloadable) entry.getNode().getArtifact()).canLiveReload();
				}
				catch (Exception e) {
					logger.warn("Can not check for live reload property", e);
				}
			}
			
			if (!entry.getId().equals(id)) {
				logger.info("Actually reloading: " + entry.getId() + " (" + recursiveReload + ")");
			}
			if (liveReload) {
				liveReload(entry);
			}
			else {
				unload(entry, false);
				preload(entry);
				load(entry);
			}
			// also reload all the dependencies
			// prevent concurrent modification
			if (recursiveReload) {
				Set<String> dependenciesToReload = calculateDependenciesToReload(entry);
				for (String dependency : dependenciesToReload) {
					// don't reload dependencies inside the entry, they have already been reloaded
					if (!dependency.startsWith(entry.getId() + ".")) {
						reload(dependency, false);
					}
				}
			}
		}
		if (recursiveReload) {
			// TODO: remove
			reattachMavenArtifacts();
			getEventDispatcher().fire(new RepositoryEvent(RepositoryState.RELOAD, true), this);
		}
		// rescan so we don't have surprises later on, otherwise the first scan might be triggered by a shutdown which will trigger an infinite reload loop
		scanForTypes();
	}
	
	private boolean canLiveReload(Entry entry) {
		try {
			return allowLiveReload && entry.isNode() && entry.getNode() != null && LiveReloadable.class.isAssignableFrom(entry.getNode().getArtifactClass()) && entry.getNode().isLoaded()
				// we don't (yet) allow for live reload of these, cause they would need to re-examine the children and (if changed), reload anyway
				&& !ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager());
		}
		catch (Exception e) {
			logger.error("Can not check live reload for: " + entry.getId(), e);
			return false;
		}
	}
	
	private void liveReload(Entry entry) {
		try {
			logger.info("Live reloading: " + entry.getId());
			((LiveReloadable) entry.getNode().getArtifact()).liveReload();
		}
		catch (Exception e) {
			logger.warn("Can not live reload artifact " + entry.getId(), e);
		}
	}
	
	private Set<String> calculateDependenciesToReload(Entry entry) {
		Set<String> dependencies = new HashSet<String>();
		if (entry.isNode()) {
			// if we can do a live reload, we don't need to reload dependencies
			if (!canLiveReload(entry)) {
				dependencies.addAll(calculateDependenciesToReload(entry.getId()));
			}
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
		return calculateDependenciesToReload(id, new HashSet<String>());
	}
	
	private Set<String> calculateDependenciesToReload(String id, Set<String> blacklist) {
		blacklist.add(id);
		List<String> directDependencies = getDependencies(id);
		Set<String> dependenciesToReload = new LinkedHashSet<String>(directDependencies);
		for (String directDependency : directDependencies) {
			if (!blacklist.contains(directDependency)) {
				Set<String> indirectDependencies = calculateDependenciesToReload(directDependency, blacklist);
				// remove any dependencies that are also in the indirect ones
				// we can add them again afterwards which means they will only be in the list once _and_ in the correct order
				dependenciesToReload.removeAll(indirectDependencies);
				dependenciesToReload.addAll(indirectDependencies);
			}
		}
		return dependenciesToReload;
	}

	private void preload(Entry entry) {
		if (isPreload(entry)) {
			try {
				Artifact artifact = entry.getNode().getArtifact();
				if (artifact instanceof LazyArtifact) {
					((LazyArtifact) artifact).forceLoad();
				}
				if (artifact instanceof ClassProvidingArtifact && !classProvidingArtifacts.contains(artifact)) {
					classProvidingArtifacts.add((ClassProvidingArtifact) artifact);
				}
			}
			catch (Exception e) {
				logger.error("Could not preload '" + entry.getId() + "'", e);
			}
		}
		for (Entry child : entry) {
			preload(child);
		}
	}
	
	private void load(Entry entry) {
		logger.info("Loading: " + entry.getId());
		List<Entry> artifactRepositoryManagers = new ArrayList<Entry>();
		load(entry, artifactRepositoryManagers);
		// first load the repositories without dependencies
		logger.info("Sorting artifact generators");
		// if we optimize, we DON'T move the ones with no references to the front as this will trigger quite a few moves
		// instead, we loop over the artifacts twice, first to load those without references, then to load those with
		// without optimize, the sort took 3.5s on an average project, with optimize, it is reduced to under 1s
		boolean optimize = true;
		sortArtifactRepositoryManagers(artifactRepositoryManagers, optimize);
		logger.info("Loading dynamically generated artifacts");
		if (!optimize) {
			for (Entry manager : artifactRepositoryManagers) {
				loadArtifactManager(manager);
			}
		}
		else {
			for (Entry manager : artifactRepositoryManagers) {
				if (manager.getNode().getReferences() == null || manager.getNode().getReferences().isEmpty()) {
					logger.info("Loading artifact manager without references: " + manager.getId());
					loadArtifactManager(manager);
				}
			}
			// then the rest
			for (Entry manager : artifactRepositoryManagers) {
				if (manager.getNode().getReferences() != null && !manager.getNode().getReferences().isEmpty()) {
					logger.info("Loading artifact manager with references: " + manager.getId());
					loadArtifactManager(manager);
				}
			}
		}
		// reset the scanned items, otherwise not all dynamically loaded artifacts are present?
		reset();
	}
	
	// in the beginning the reference check was not recursive, but then we had a case where an sql service was dependent on a structure which extended the output of a CRUD
	// the sql service did not see the indirect reference to the CRUD and was loaded before the CRUD
	// this forced the structure to load "early" at which point it could not resolve the reference to the CRUD (which was not yet loaded)
	// this corrupted the document...
	// recursive reference checking _will_ pick this up but slowed down the sort from sub-second to 11s on a pretty standard setup
	// however, not _all_ references are relevant, we only want to determine dependencies between artifact managers, so if we strip down the references to only artifact managers, that can speed up the process
	// additionally a lot of managers reference themselves (sometimes indirectly) so we remove those references as well
	// this allowed us to get from 468 interdependencies to just 16 which massively updated performance of the sorting to sub 200ms
	private Set<String> cleanupArtifactRepositoryManagerReferences(String currentId, List<String> artifactRepositoryManagerIds, Set<String> allReferences) {
		Set<String> result = new HashSet<String>();
		Iterator<String> iterator = allReferences.iterator();
		outer: while (iterator.hasNext()) {
			String next = iterator.next();
			if (next == null) {
				continue;
			}
			// if there is a direct reference, we retain it
			if (artifactRepositoryManagerIds.contains(next)) {
				result.add(next);
				continue;
			}
			int index = -1;
			do {
				index = next.lastIndexOf('.');
				if (index >= 0) {
					next = next.substring(0, index);
					// if we have a reference to a child of one, we also keep it
					if (artifactRepositoryManagerIds.contains(next)) {
						// not if it's yourself you don't!
						// we don't continue outer just in case we ever want to do _really_ funky things with artifact managers that generate new artifact managers (probably not?)
						if (currentId.equals(next)) {
							continue;
						}
						result.add(next);
						continue outer;
					}
				}
			}
			while (index >= 0);
		}
		return result;
	}
	
	private void sortArtifactRepositoryManagers(List<Entry> artifactRepositoryManagers, boolean optimize) {
		// keep track of the original list
		List<Entry> original = artifactRepositoryManagers;
		
		// if we optimize, we don't care about all the repository managers that have no references, they will be loaded first anyway
		// if we remove them now, the especially important part is that they don't show up in the references of the other artifact managers, so whilst for example the maven repository managers are "few" (in comparison)
		// they do represent a reference for pretty much every other repository manager
		// by assuming they will always be loaded first (which is the case in the optimize), we don't need to explicitly check that
		// so our "cleaned up" artifact reference list will be a _lot_ smaller
		// as a reference point, 468 entries (with complex interdependencies) are resolved in +- 11s on an i9
		// by doing this optimize routine, we can reduce the actually checked managers to 306 in 2s
		if (optimize) {
			List<Entry> partList = new ArrayList<Entry>();
			for (Entry single : artifactRepositoryManagers) {
				if (single.getNode().getReferences() == null || single.getNode().getReferences().isEmpty()) {
					continue;
				}
				partList.add(single);
			}
			artifactRepositoryManagers = partList;
		}
		Map<String, Set<String>> allReferences = new HashMap<String, Set<String>>();
		List<String> artifactRepositoryManagerIds = new ArrayList<String>();
		for (Entry entry : artifactRepositoryManagers) {
			artifactRepositoryManagerIds.add(entry.getId());
		}
		// again we take a subpart of the full list, we are only interested in sorting the ones that have interdependencies
		List<Entry> partList = new ArrayList<Entry>();
		for (int i = 0; i < artifactRepositoryManagers.size(); i++) {
			// this happens _after_ the load so it _should_ contain all the references already
			Set<String> references = EAIRepositoryUtils.getAllReferences(this, artifactRepositoryManagers.get(i).getId());
			if (!references.isEmpty()) {
				Set<String> cleanupArtifactRepositoryManagerReferences = cleanupArtifactRepositoryManagerReferences(artifactRepositoryManagers.get(i).getId(), artifactRepositoryManagerIds, references);
				if (!cleanupArtifactRepositoryManagerReferences.isEmpty()) {
					allReferences.put(artifactRepositoryManagers.get(i).getId(), cleanupArtifactRepositoryManagerReferences);
					partList.add(artifactRepositoryManagers.get(i));
				}
			}
		}
		artifactRepositoryManagers = partList;

		boolean changed = true;
		sorting: while(changed) {
			changed = false;
			
			// if we check the first half we should be fine
			// Update @ 2020-09-03: suppose you have 10 artifacts and 8 depends on 9. this would not get picked up by only scanning the first half and comparing it to the second...? not sure why this was implemented as such...
//			for (int i = 0; i < (artifactRepositoryManagers.size() % 2 == 0 ? artifactRepositoryManagers.size() / 2 : (artifactRepositoryManagers.size() / 2) + 1); i++) {
			for (int i = 0; i < artifactRepositoryManagers.size(); i++) {
				for (int j = i + 1; j < artifactRepositoryManagers.size(); j++) {
					if (i == j) {
						continue;
					}
					int result = 0;
					Entry o1 = artifactRepositoryManagers.get(i);
					Entry o2 = artifactRepositoryManagers.get(j);
					boolean o1Has = allReferences.containsKey(o1.getId());
					boolean o2Has = allReferences.containsKey(o2.getId());
					boolean o1DependsOnO2 = o1Has && allReferences.get(o1.getId()).contains(o2.getId());
					boolean o2DependsOnO1 = o2Has && allReferences.get(o2.getId()).contains(o1.getId());						
					if (o1DependsOnO2 && !o2DependsOnO1) {
						result = 1;
					}
					else if (!o1DependsOnO2 && o2DependsOnO1) {
						result = -1;
					}
					else if (o1DependsOnO2 && o2DependsOnO1) {
						logger.warn("Found a circular dependency between " + o1.getId() + " and " + o2.getId());
					}
					// we need to switch them
					if ((i < j && result == 1) || (i > j && result == -1)) {
						artifactRepositoryManagers.set(i, o2);
						artifactRepositoryManagers.set(j, o1);
						changed = true;
						continue sorting;
					}
				}
			}
		}
		// so the original has tons more, but these are either without any references at all (which are loaded first anyway)
		// or with references that are _not_ to other artifact managers
		// the ones we sorted only contain artifact managers with references to one another
		// we now remove them from the original list and re-add them in the correct order
		original.removeAll(artifactRepositoryManagers);
		original.addAll(artifactRepositoryManagers);
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

	private boolean isPreload(Entry entry) {
		if (entry instanceof ResourceEntry) {
			Resource resource = ((ResourceEntry) entry).getContainer().getChild("node.properties");
			if (resource != null) {
				Properties properties = new Properties();
				try {
					ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
					try {
						properties.load(IOUtils.toInputStream(readable, true));
					}
					finally {
						readable.close();
					}
					String stage = properties.getProperty("stage", "load");
					return "preload".equals(stage) && entry.isNode();
				}
				catch (Exception e) {
					logger.error("Could not preload '" + entry.getId() + "'", e);
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings({ "rawtypes" })
	private void load(Entry entry, List<Entry> artifactRepositoryManagers) {
		// don't refresh on initial load, this messes up performance for remote file systems
		if (!isLoading && !isPreload(entry)) {
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
				try {
					load(child, artifactRepositoryManagers);
				}
				catch (Exception e) {
					logger.error("Can not load: " + child.getId(), e);
				}
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
		// always do a broken reference update, there are too many edge cases otherwise (e.g. a folder of rest services)
		if (allowFileBasedRefactor && newEntry instanceof ResourceEntry) {
			EAIRepositoryUtils.updateBrokenReferences(((ResourceEntry) newEntry).getContainer(), entry.getId(), newEntry.getId(), charset);
		}
		// if we have a node, check if it is a container artifact (for the renaming of the rest service)
		else if (newEntry.isNode()) {
			remapContainerArtifactAfterRename(entry, newEntry);
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
	
	private Set<String> relink(Entry from, Entry to, List<Validation<?>> validations, boolean reload) {
		logger.debug("Relinking: " + from.getId() + " to " + to.getId());
		Set<String> dependencies = new HashSet<String>(getDependencies(from.getId()));
		if (dependencies != null) {
			for (String dependency : dependencies) {
				logger.debug("Relinking dependency: " + dependency);
				Entry dependencyEntry = getEntry(dependency);
				relinkSingle(from, to, validations, dependencyEntry);
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
			reloadAll(dependencies, true);
		}
		return dependencies;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void remapContainerArtifactAfterRename(Entry from, Entry to) {
		// containers are...tricky
		// when we rename them, they might have dependencies to one another, e.g. the blox service will have an interface dependency to the rest interface
		// they all get renamed but don't know (from each other) that they are updated as they are not listed as dependencies to themselves
		// using "classic" updateReference logic is impossible but we can't successfully load the renamed (because the old references don't resolve)
		// for this reason, we do a hard broken update at this point
		// it is not ideal but there are very few good ways at this point
		// it was not a problem for a long time because internal references were done using $self
		// it was decided to step away from the $self logic (commit 68f9a0541eee95896e134aec5eb2c4a79f08a7c7) for better logging (amongst other things)
		// if we can't make this work, we may have to revert the $self
		if (to.isNode() && ContainerArtifactManager.class.isAssignableFrom(to.getNode().getArtifactManager()) && to instanceof ResourceEntry) {
			logger.info("Relinking internal artifacts in container");
			try {
				ContainerArtifactManager manager = (ContainerArtifactManager) to.getNode().getArtifactManager().newInstance();
				manager.updateBrokenReference(((ResourceEntry) to).getContainer(), from.getId(), to.getId());
			}
			catch (Exception e) {
				logger.error("Could not relink internal artifacts for container", e);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void relinkSingle(Entry from, Entry to, List<Validation<?>> validations, Entry dependencyEntry) {
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
					reload(dependencyEntry.getId(), false);
				}
				catch (Exception e) {
					logger.error("Could not update reference for dependency '" + dependencyEntry.getId() + "' from '" + from.getId() + "' to '" + to.getId() + "'", e);
				}
			}
		}
		else {
			validations.add(new ValidationMessage(Severity.ERROR, "Can not update dependency '" + dependencyEntry.getId() + "' as it is not a resource entry"));
		}
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

	/**
	 * This is meant for dispatching low volume events that can impact the system itself
	 * For example repository loading/unloading which may trigger the server to take action
	 * Or notifications which may lead to emails or other forms of notifications
	 * 
	 * This is _not_ meant for high volume events that need further rule evaluation to potentially conclude into a structural event 
	 */
	@Override
	public EventDispatcher getEventDispatcher() {
		return dispatcher;
	}
	
	@Override
	public EventDispatcher getComplexEventDispatcher() {
		return complexDispatcher;
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
		return name.matches("^[a-zA-Z]+[\\w]+$") && !RESERVED.contains(name);
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
	
	@Override
	public void register(DomainRepository domainRepository) {
		mavenRepositories.add(domainRepository);
		startMavenRepository(domainRepository);
	}
	
	@Override
	public void unregister(DomainRepository domainRepository) {
		// unload all maven artifacts related to this repository
		Iterator<MavenArtifact> artifactIterator = mavenArtifacts.iterator();
		while (artifactIterator.hasNext()) {
			MavenArtifact artifact = artifactIterator.next();
			if (artifact.getRepository().equals(domainRepository)) {
				try {
					MavenManager.detachChildren(getRoot(), artifact);
				}
				catch (IOException e) {
					logger.error("Could not properly unload maven artifact: " + artifact.getId(), e);
				}
				artifactIterator.remove();
			}
		}
		// unload all the maven managers related to this repository
		Iterator<MavenManager> managerIterator = mavenManagers.iterator();
		while (managerIterator.hasNext()) {
			MavenManager mavenManager = managerIterator.next();
			if (mavenManager.getRepository().equals(domainRepository)) {
				managerIterator.remove();
			}
		}
		mavenRepositories.remove(domainRepository);
	}
	
	private void startMavenRepository(ResourceContainer<?> target) throws IOException {
		logger.info("Starting maven repository located at: " + ResourceUtils.getURI(target));
		be.nabu.libs.maven.ResourceRepository mavenRepository = new be.nabu.libs.maven.ResourceRepository(target, getEventDispatcher());
		mavenRepository.getDomains().addAll(internalDomains);
		mavenRepository.scan(false);
		mavenRepositories.add(mavenRepository);
		startMavenRepository(mavenRepository);
	}

	private void startMavenRepository(DomainRepository mavenRepository) {
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
			MavenArtifact artifact = mavenManager.load(this, internal, updateMavenSnapshots, localMavenServer);
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
			private String correlationId;
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
			@Override
			public String getCorrelationId() {
				if (CorrelationIdEnricher.getCorrelationId() != null) {
					return CorrelationIdEnricher.getCorrelationId();
				}
				else if (correlationId == null) {
					synchronized(this) {
						if (correlationId == null) {
							correlationId = UUID.randomUUID().toString().replace("-", "");
						}
					}
				}
				return correlationId;
			}
		};
	}
	
	public CacheProvider getCacheProvider() {
		return cacheProvider;
	}

	@Override
	public ExecutionContext newExecutionContext(Token primaryToken, Token...alternativeTokens) {
		return new EAIExecutionContext(this, primaryToken, isDevelopment(), alternativeTokens);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getArtifacts(Class<T> ifaceClass) {
		List<T> results = new ArrayList<T>();
		Map<Class<? extends Artifact>, Map<String, Node>> nodesByType = this.nodesByType;
		if (nodesByType == null) {
			nodesByType = scanForTypes();
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
						logger.error("Could not load artifact: " + node, e);
					}
				}
			}
		}
		return results;
	}
	
	protected List<Node> getNodes(Class<?> artifactClazz) {
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
	
	public Map<Class<? extends Artifact>, Map<String, Node>> scanForTypes() {
		nodesByType = null;
		return scanForTypes(repositoryRoot);
	}
	
	public Map<Class<? extends Artifact>, Map<String, Node>> scanForTypes(Entry entry) {
		Map<Class<? extends Artifact>, Map<String, Node>> nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
		Map<Class<? extends Artifact>, Map<String, Node>> existing = this.nodesByType;
		if (existing != null) {
			nodesByType.putAll(existing);
		}
		for (Entry child : entry) {
			if (child.isNode()) {
				Class<? extends Artifact> artifactClass = child.getNode().getArtifactClass();
				if (!nodesByType.containsKey(artifactClass)) {
					nodesByType.put(artifactClass, new HashMap<String, Node>());
				}
				nodesByType.get(artifactClass).put(child.getId(), child.getNode());
			}
			if (!child.isLeaf()) {
				scanForTypes(child, nodesByType);
			}
		}
		this.nodesByType = nodesByType;
		return nodesByType;
	}
	
	private void scanForTypes(Entry entry, Map<Class<? extends Artifact>, Map<String, Node>> nodesByType) {
		for (Entry child : entry) {
			try {
				if (child.isNode()) {
					Class<? extends Artifact> artifactClass = child.getNode().getArtifactClass();
					if (!nodesByType.containsKey(artifactClass)) {
						nodesByType.put(artifactClass, new HashMap<String, Node>());
					}
					nodesByType.get(artifactClass).put(child.getId(), child.getNode());
				}
			}
			catch (Exception e) {
				// if we can't load it, we ignore it, otherwise it can create endless problems
				logger.error("Can not scan: " + child.getId(), e);
				child.refresh(false);
			}
			if (!child.isLeaf()) {
				scanForTypes(child, nodesByType);
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
		if (mavenRoot != null) {
			try {
				startMavenRepository(mavenRoot);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// before we start loading everything else, first do an initial attach so all the artifacts are loaded (for dependencies)
		reattachMavenArtifacts();
		isLoading = true;
		preload(repositoryRoot);
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
		return getArtifacts(DefinedService.class);
	}
	
	Class<?> loadClass(String name) throws ClassNotFoundException {
		for (MavenArtifact artifact : mavenArtifacts) {
			try {
				Class<?> clazz = artifact.getClassLoader().loadClassNonRecursively(name);
				if (clazz != null) {
					return clazz;
				}
			}
			catch (ClassNotFoundException e) {
				// ignore
			}
		}
		for (ClassProvidingArtifact artifact : classProvidingArtifacts) {
			for (LocalClassLoader classLoader : artifact.getClassLoaders()) {
				try {
					Class<?> loadClass = classLoader.loadClassNonRecursively(name);
					if (loadClass != null) {
						return loadClass;
					}
				}
				catch (Exception e) {
					logger.error("Could not search artifact '" + artifact.getId()  + "' for classes", e);
				}
			}
		}
		return null;
	}
	
	URL getResource(String name) {
		for (MavenArtifact artifact : mavenArtifacts) {
			Collection<URL> resources = artifact.getClassLoader().findResourcesNonRecursively(name, true);
			if (!resources.isEmpty()) {
				return resources.iterator().next();
			}
		}
		for (ClassProvidingArtifact artifact : classProvidingArtifacts) {
			for (LocalClassLoader classLoader : artifact.getClassLoaders()) {
				Collection<URL> resources = classLoader.findResourcesNonRecursively(name, true);
				if (!resources.isEmpty()) {
					return resources.iterator().next();
				}

			}
		}
		return null;
	}
	
	private Map<String, Collection<URL>> resources = new HashMap<String, Collection<URL>>();
	
	Collection<URL> getResources(String name) {
		if (!resources.containsKey(name)) {
			synchronized(resources) {
				if (!resources.containsKey(name)) {
					Set<URL> urls = new LinkedHashSet<URL>();
					List<URL> tmp = new ArrayList<URL>();
					for (MavenArtifact artifact : mavenArtifacts) {
						tmp.addAll(artifact.getClassLoader().findResourcesNonRecursively(name, false));
					}
					for (ClassProvidingArtifact artifact : classProvidingArtifacts) {
						for (LocalClassLoader classLoader : artifact.getClassLoaders()) {
							tmp.addAll(classLoader.findResourcesNonRecursively(name, false));
						}
					}
					urls.addAll(tmp);
					resources.put(name, urls);
				}
			}
		}
		return resources.get(name);
	}
	
	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
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
						metrics.put(id, new MetricGrouper(newMetricInstance(id), new MetricsLevelProvider(id)));
					}
				}
			}
			return metrics.get(id);
		}
		return null;
	}
	
	public void enableMetrics(boolean enable) {
		if (enable && metrics == null) {
			RepositoryThreadFactory repositoryThreadFactory = new RepositoryThreadFactory(this, true);
			repositoryThreadFactory.setName("metrics-events");
			// create a new metrics dispatching pool, allowing you to intercept metric updates
			metricsDispatcher = new EventDispatcherImpl(Executors.newFixedThreadPool(2, repositoryThreadFactory));
			synchronized(this) {
				if (metrics == null) {
					metrics = new HashMap<String, MetricGrouper>();
					synchronized(metrics) {
						MetricGrouper metric = new MetricGrouper(newMetricInstance(METRICS_SYSTEM), new MetricsLevelProvider(METRICS_SYSTEM));
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
	
	private MetricInstanceImpl newMetricInstance(String id) {
		if (metricsGaugeHistorizer == null && historizeGauges) {
			synchronized(this) {
				if (metricsGaugeHistorizer == null) {
					metricsGaugeHistorizer = new GaugeHistorizer(historizationInterval);
					// start the historizer thread
					new Thread(metricsGaugeHistorizer).start();
				}
			}
		}
		MetricInstanceImpl instance = new MetricInstanceImpl(id, this, metricsDispatcher);
		// by default there are no events
		instance.setEnableEvents(enableEvents.contains(id));
		if (metricsGaugeHistorizer != null) {
			metricsGaugeHistorizer.add(instance);
		}
		return instance;
	}
	
	public Collection<String> getMetricInstances() {
		return metrics == null ? null : metrics.keySet();
	}

	@Override
	public Sink getSink(String id, String category) {
		String key = id + ":" + category;
		if (!sinks.containsKey(key)) {
			synchronized(this) {
				if (!sinks.containsKey(key)) {
					sinks.put(key, new LimitedHistorySinkWithStatistics(historySize));
				}
			}
		}
		return sinks.get(key);
	}

	@Override
	public EventDispatcher getMetricsDispatcher() {
		return metricsDispatcher;
	}

	@Override
	public String getName() {
		return name == null ? MavenRepository.super.getName() : name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getGroup() {
		return group == null ? MavenRepository.super.getGroup() : group;
	}
	public void setGroup(String group) {
		this.group = group;
	}

	public RoleHandler getRoleHandler() {
		return roleHandler;
	}

	public void setRoleHandler(RoleHandler roleHandler) {
		this.roleHandler = roleHandler;
	}

	public PermissionHandler getPermissionHandler() {
		return permissionHandler;
	}

	public void setPermissionHandler(PermissionHandler permissionHandler) {
		this.permissionHandler = permissionHandler;
	}

	@Override
	public LicenseManager getLicenseManager() {
		return licenseManager;
	}
	public void setLicenseManager(LicenseManager licenseManager) {
		this.licenseManager = licenseManager;
	}

	public boolean isHistorizeGauges() {
		return historizeGauges;
	}

	public void setHistorizeGauges(boolean historizeGauges) {
		this.historizeGauges = historizeGauges;
	}

	public long getHistorizationInterval() {
		return historizationInterval;
	}

	public void setHistorizationInterval(long historizationInterval) {
		this.historizationInterval = historizationInterval;
	}

	public int getHistorySize() {
		return historySize;
	}

	public void setHistorySize(int historySize) {
		this.historySize = historySize;
	}

	@Override
	public Map<String, List<Validation<?>>> getMessages(String nodeId) {
		if (!messages.containsKey(nodeId)) {
			synchronized(messages) {
				if (!messages.containsKey(nodeId)) {
					messages.put(nodeId, new ConcurrentHashMap<String, List<Validation<?>>>());
				}
			}
		}
		return messages.get(nodeId);
	}

	@Override
	public Map<String, List<String>> getSinks() {
		Map<String, List<String>> sinks = new HashMap<String, List<String>>();
		for (String id : getMetricInstances()) {
			MetricInstanceImpl instance = (MetricInstanceImpl) getMetricInstance(id).getParent();
			Set<String> ids = new TreeSet<String>();
			ids.addAll(instance.getGaugeIds());
			ids.addAll(instance.getSinkIds());
			sinks.put(id, new ArrayList<String>(ids));
		}
		return sinks;
	}

	@Override
	public List<String> getAliases() {
		return aliases;
	}
	
	public List<String> getEnabledFeatures() {
		List<String> enabledFeatures = new ArrayList<String>();
		for (FeatureConfigurator configurator : getArtifacts(FeatureConfigurator.class)) {
			List<String> enabled = configurator.getEnabledFeatures();
			if (enabled != null) {
				enabledFeatures.addAll(enabled);
			}
		}
		return enabledFeatures;
	}

	public void addEventEnricher(String name, EventEnricher enricher) {
		synchronized(eventEnrichers) {
			eventEnrichers.put(name, enricher);
		}
	}
	
	public void removeEventEnricher(String name) {
		synchronized(eventEnrichers) {
			eventEnrichers.remove(name);
		}
	}

	public List<Templater> getTemplaters() {
		return templaters;
	}
	
}

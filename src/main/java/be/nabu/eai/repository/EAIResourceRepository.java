package be.nabu.eai.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.events.RepositoryEvent;
import be.nabu.eai.repository.events.RepositoryEvent.RepositoryState;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.maven.api.DomainRepository;
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
import be.nabu.libs.services.SPIDefinedServiceInterfaceResolver;
import be.nabu.libs.services.ServiceRunnerFactory;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.types.DefinedSimpleTypeResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.SPIDefinedTypeResolver;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * Ideally at some point I want to plug in the higher level VFS api instead of the low level resource API
 * To make this easier, I have tried to encapsulate as much resource-related stuff here as possible
 * 
 * TODO: add a node event listener for rename, move, delete etc node events to update reference?
 * 		> currently they are static
 */
public class EAIResourceRepository implements ResourceRepository {
	
	private ResourceContainer<?> resourceRoot;
	private RepositoryEntry repositoryRoot;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	
	private static EAIResourceRepository instance;
	
	public static final String PRIVATE = "private";
	public static final String PUBLIC = "public";
	
	private Map<Class<? extends Artifact>, Map<String, Node>> nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static List<String> INTERNAL = Arrays.asList(new String [] { PRIVATE, PUBLIC });
	private static List<String> RESERVED = Arrays.asList(new String [] { PRIVATE, PUBLIC, "class", "package", "import", "for", "while", "if", "do", "else" });
	private Charset charset = Charset.forName("UTF-8");
	private DomainRepository mavenRepository;
	private ResourceContainer<?> mavenRoot;
	private URI localMavenServer;
	private boolean updateMavenSnapshots = false;
	
	public EAIResourceRepository() throws IOException, URISyntaxException {
		this(
			(ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/repository")), null),
			(ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/maven")), null)
		);
	}
	
	public EAIResourceRepository(ResourceContainer<?> repositoryRoot, ResourceContainer<?> mavenRoot) throws IOException {
		this.resourceRoot = repositoryRoot;
		this.mavenRoot = mavenRoot;
		this.repositoryRoot = new RepositoryEntry(this, repositoryRoot, null, "/");
		ArtifactResolverFactory.getInstance().addResolver(this);
		DefinedTypeResolverFactory.getInstance().addResolver(new EAIRepositoryTypeResolver(this));
		DefinedTypeResolverFactory.getInstance().addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
		DefinedTypeResolverFactory.getInstance().addResolver(new SPIDefinedTypeResolver());
		DefinedServiceResolverFactory.getInstance().addResolver(new EAIRepositoryServiceResolver(this));
		// service interface resolvers
		DefinedServiceInterfaceResolverFactory.getInstance().addResolver(new EAIRepositoryServiceInterfaceResolver(this));
		DefinedServiceInterfaceResolverFactory.getInstance().addResolver(new SPIDefinedServiceInterfaceResolver());
		instance = this;
	}
	
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
		Entry entry = getEntry(id);
		return entry != null && entry.isNode() ? entry.getNode() : null;
	}
	
	private Map<String, List<String>> references = new HashMap<String, List<String>>(), dependencies = new HashMap<String, List<String>>();
	
	private void buildReferenceMap(String id, List<String> references) {
		logger.info("Loading references for '" + id + "': " + references);
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
	
	public List<String> getDependencies(String id) {
		return dependencies.get(id);
	}
	
	public List<String> getReferences(String id) {
		return references.get(id);
	}
	
	@Override
	public Charset getCharset() {
		return charset;
	}
	
	public void unload(String id) {
		Entry entry = getEntry(id);
		if (entry != null) {
			unload(entry);
			entry.getParent().refresh();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void unload(Entry entry) {
		logger.info("Unloading: " + entry.getId());
		nodesByType = null;
		if (entry.isNode()) {
			// if there is an artifact manager and it maintains a repository, remove it all
			if (entry.getNode().getArtifactManager() != null && ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager())) {
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
			// TODO: remove from reference map?
		}
		if (!entry.isLeaf()) {
			for (Entry child : entry) {
				unload(child);
			}
		}
	}
	
	public void reload(String id) {
		logger.info("Reloading: " + id);
		Entry entry = getEntry(id);
		while (entry == null && id.contains(".")) {
			int index = id.lastIndexOf('.');
			id = id.substring(0, index);
			entry = getEntry(id);
		}
		if (entry != null) {
			unload(entry);
			entry.refresh();
			load(entry);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void load(Entry entry) {
		// reset this to make sure any newly loaded entries are picked up or old entries are deleted
		nodesByType = null;
		if (entry.isNode()) {
			logger.info("Loading entry: " + entry.getId());
			buildReferenceMap(entry.getId(), entry.getNode().getReferences());
			if (entry instanceof ModifiableEntry && entry.isNode() && entry.getNode().getArtifactManager() != null && ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager())) {
				try {
					List<Entry> addedChildren = ((ArtifactRepositoryManager) entry.getNode().getArtifactManager().newInstance()).addChildren((ModifiableEntry) entry, entry.getNode().getArtifact());
					if (addedChildren != null) {
						for (Entry addedChild : addedChildren) {
							buildReferenceMap(addedChild.getId(), addedChild.getNode().getReferences());
						}
					}
				}
				catch(RuntimeException e) {
					logger.error("Could not finish loading generated children for " + entry.getId(), e);
				}
				catch (InstantiationException e) {
					logger.error("Could not finish loading generated children for " + entry.getId(), e);
				}
				catch (IllegalAccessException e) {
					logger.error("Could not finish loading generated children for " + entry.getId(), e);
				}
				catch (IOException e) {
					logger.error("Could not finish loading generated children for " + entry.getId(), e);
				}
				catch (ParseException e) {
					logger.error("Could not finish loading generated children for " + entry.getId(), e);
				}
			}
		}
		if (!entry.isLeaf()) {
			for (Entry child : entry) {
				load(child);
			}
		}
	}

	private Entry getEntry(String id) {
		ParsedPath path = new ParsedPath(id.replace('.', '/'));
		Entry entry = getRoot();
		while (entry != null && path != null) {
			entry = entry.getChild(path.getName());
			path = path.getChildPath();
		}
		return entry;
	}
	
	@Override
	public Artifact resolve(String id) {
		Entry entry = getEntry(id);
		try {
			return entry != null && entry.isNode() ? entry.getNode().getArtifact() : null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
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
	
	private void startMavenRepository(ResourceContainer<?> target) throws IOException {
		logger.info("Starting maven repository located at: " + ResourceUtils.getURI(target));
		mavenRepository = new be.nabu.libs.maven.ResourceRepository(target, getEventDispatcher());
		// everything in the "nabu" domain is considered internal
		// TODO: allow configurable entries
		mavenRepository.getDomains().add("nabu");
		mavenRepository.scan();
		
		// do an initial load of all internal artifacts
		for (be.nabu.libs.maven.api.Artifact internal : mavenRepository.getInternalArtifacts()) {
			logger.info("Loading maven artifact " + internal.getGroupId() + " > " + internal.getArtifactId());
			MavenManager manager = new MavenManager(DefinedTypeResolverFactory.getInstance().getResolver());
			MavenArtifact artifact = manager.load(mavenRepository, internal, localMavenServer, updateMavenSnapshots);
			manager.addChildren(getRoot(), artifact);
		}
	}
	
	public be.nabu.libs.maven.api.DomainRepository getMavenRepository() {
		return mavenRepository;
	}
	
	public ServiceContext getServiceContext() {
		return new ServiceContext() {
			@SuppressWarnings("unchecked")
			@Override
			public <T extends Artifact> ArtifactResolver<T> getResolver(Class<T> arg0) {
				return (ArtifactResolver<T>) EAIResourceRepository.this;
			}
		};
	}
	
	@Override
	public ExecutionContext newExecutionContext(Principal principal) {
		return new EAIExecutionContext(this, principal);
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
	
	private void scanForTypes() {
		if (nodesByType == null) {
			synchronized(this) {
				nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
			}
		}
		synchronized(nodesByType) {
			nodesByType.clear();
			scanForTypes(repositoryRoot);
		}
	}
	
	private void scanForTypes(Entry entry) {
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
		load(repositoryRoot);
		getEventDispatcher().fire(new RepositoryEvent(RepositoryState.LOAD, true), this);
	}

	public ResourceContainer<?> getMavenRoot() {
		return mavenRoot;
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
}

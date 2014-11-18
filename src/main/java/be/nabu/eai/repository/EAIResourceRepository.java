package be.nabu.eai.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.ParseException;
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
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.events.EventDispatcherImpl;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.maven.CreateResourceRepositoryEvent;
import be.nabu.libs.maven.DeleteResourceRepositoryEvent;
import be.nabu.libs.maven.MavenListener;
import be.nabu.libs.resources.ResourceFactory;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.ResourceRoot;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.types.DefinedSimpleTypeResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.SPIDefinedTypeResolver;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.utils.http.api.HTTPRequest;
import be.nabu.utils.http.api.server.HTTPServer;
import be.nabu.utils.http.server.DefaultHTTPServer;
import be.nabu.utils.io.ContentTypeMap;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * Ideally at some point I want to plug in the higher level VFS api instead of the low level resource API
 * To make this easier, I have tried to encapsulate as much resource-related stuff here as possible
 */
public class EAIResourceRepository implements ArtifactResolver<Artifact>, ResourceRepository {
	
	private ManageableContainer<?> resourceRoot;
	private RepositoryEntry repositoryRoot;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	
	public static final String PRIVATE = "private";
	public static final String PUBLIC = "public";
	
	private Map<Class<? extends Artifact>, Map<String, Node>> nodesByType = new HashMap<Class<? extends Artifact>, Map<String, Node>>();
	private Map<String, Node> nodes = new HashMap<String, Node>();
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static List<String> INTERNAL = Arrays.asList(new String [] { PRIVATE, PUBLIC });
	private static List<String> RESERVED = Arrays.asList(new String [] { PRIVATE, PUBLIC, "class", "package", "import", "for", "while", "if", "do", "else" });
	private Charset charset = Charset.forName("UTF-8");
	private be.nabu.libs.maven.api.DomainRepository mavenRepository;
	
	public EAIResourceRepository() throws IOException, URISyntaxException {
		this((ManageableContainer<?>) ResourceFactory.getInstance().resolve(new URI(System.getProperty("repository.uri", "file:/" + System.getProperty("user.home") + "/repository")), null));
	}
	
	public EAIResourceRepository(ManageableContainer<?> root) throws IOException, URISyntaxException {
		this.resourceRoot = root;
		this.repositoryRoot = new RepositoryEntry(this, root, null, "/");
		ArtifactResolverFactory.getInstance().addResolver(this);
		DefinedTypeResolverFactory.getInstance().addResolver(new EAIRepositoryTypeResolver(this));
		DefinedTypeResolverFactory.getInstance().addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
		DefinedTypeResolverFactory.getInstance().addResolver(new SPIDefinedTypeResolver());
		DefinedServiceResolverFactory.getInstance().addResolver(new EAIRepositoryServiceResolver(this));
		
		load(repositoryRoot);
		
		// TODO: allow configuration of port
		Thread mavenThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					URI uri = new URI(System.getProperty("maven.repository.uri", "file:" + System.getProperty("user.home") + "/maven"));
					logger.info("Starting maven repository at " + uri + " (TODO: add to settings)");
					ResourceRoot mavenRoot = ResourceFactory.getInstance().resolve(uri, null);
					if (mavenRoot == null) {
						throw new RuntimeException("Can not find the maven root, currently hardcoded as: file:" + System.getProperty("user.home") + "/maven");
					}
					startMavenListener(5555, (ResourceContainer<?>) mavenRoot);
				}
				catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		mavenThread.setDaemon(true);
		mavenThread.start();
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
		return nodes.get(id);
	}
	
	public void load() {
		load(repositoryRoot);
	}
	
	private void buildReferenceMap(String id, List<String> references) {
		// TODO
	}	
	
	public Charset getCharset() {
		return charset;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void load(Entry entry) {
		for (Entry child : entry) {
			if (child.isNode()) {
				logger.info("Loading entry: " + child.getId());
				buildReferenceMap(child.getId(), child.getNode().getReferences());
				Class<? extends Artifact> artifactClass = child.getNode().getArtifactClass();
				if (!nodesByType.containsKey(artifactClass)) {
					nodesByType.put(artifactClass, new HashMap<String, Node>());
				}
				nodesByType.get(artifactClass).put(child.getId(), child.getNode());
				nodes.put(child.getId(), child.getNode());
				if (child instanceof ModifiableEntry && ArtifactRepositoryManager.class.isAssignableFrom(child.getNode().getArtifactManager())) {
					try {
						((ArtifactRepositoryManager) child.getNode().getArtifactManager().newInstance()).addChildren((ModifiableEntry) child, child.getNode().getArtifact());
					}
					catch (InstantiationException e) {
						throw new RuntimeException(e);
					}
					catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					catch (ParseException e) {
						throw new RuntimeException(e);
					}
				}
			}
			else if (!child.isLeaf()) {
				load(child);
			}
		}
	}

	@Override
	public Artifact resolve(String id) {
		ParsedPath path = new ParsedPath(id.replace('.', '/'));
		Entry entry = getRoot();
		while (entry != null && path != null) {
			entry = entry.getChild(path.getName());
			path = path.getChildPath();
		}
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
		return name.matches("^[\\w]+$") && !RESERVED.contains(name);
	}
	
	private void startMavenListener(int port, ResourceContainer<?> target) throws IOException {
		HTTPServer server = new DefaultHTTPServer(port, 10, getEventDispatcher());
		mavenRepository = new be.nabu.libs.maven.ResourceRepository(target, getEventDispatcher());
		// everything in the "nabu" domain is considered internal
		// TODO: allow configurable entries
		mavenRepository.getDomains().add("nabu");
		mavenRepository.scan();
		
		// do an initial load of all internal artifacts
		for (be.nabu.libs.maven.api.Artifact internal : mavenRepository.getInternalArtifacts()) {
			logger.info("Loading maven artifact " + internal.getGroupId() + " > " + internal.getArtifactId());
			MavenManager manager = new MavenManager(DefinedTypeResolverFactory.getInstance().getResolver());
			MavenArtifact artifact = manager.load(mavenRepository, internal);
			manager.addChildren(getRoot(), artifact);
		}
		
		getEventDispatcher().subscribe(DeleteResourceRepositoryEvent.class, new EventHandler<DeleteResourceRepositoryEvent, Void>() {
			@Override
			public Void handle(DeleteResourceRepositoryEvent event) {
				logger.info("Deleting maven artifact " + event.getArtifact().getArtifactId());
				MavenManager manager = new MavenManager(DefinedTypeResolverFactory.getInstance().getResolver());
				try {
					manager.remove(getRoot(), manager.load(mavenRepository, event.getArtifact()));
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		}).filter(new EventHandler<DeleteResourceRepositoryEvent, Boolean>() {
			@Override
			public Boolean handle(DeleteResourceRepositoryEvent event) {
				return !event.isInternal();
			}
		});
		getEventDispatcher().subscribe(CreateResourceRepositoryEvent.class, new EventHandler<CreateResourceRepositoryEvent, Void>() {
			@Override
			public Void handle(CreateResourceRepositoryEvent event) {
				logger.info("Installing maven artifact " + event.getArtifact().getArtifactId());
				MavenManager manager = new MavenManager(DefinedTypeResolverFactory.getInstance().getResolver());
				MavenArtifact artifact = manager.load(getMavenRepository(), event.getArtifact());
				try {
					manager.addChildren(getRoot(), artifact);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		}).filter(new EventHandler<CreateResourceRepositoryEvent, Boolean>() {
			@Override
			public Boolean handle(CreateResourceRepositoryEvent event) {
				return !event.isInternal();
			}
		});
		// no support for non-root calls atm!
		server.getEventDispatcher().subscribe(HTTPRequest.class, new MavenListener(mavenRepository));
		server.start();
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
	
	public ExecutionContext newExecutionContext(Principal principal) {
		return new EAIExecutionContext(this, principal);
	}
}

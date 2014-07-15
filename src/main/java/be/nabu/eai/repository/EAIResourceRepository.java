package be.nabu.eai.repository;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.events.EventDispatcherImpl;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.types.DefinedSimpleTypeResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.SPIDefinedTypeResolver;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
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
	
	public EAIResourceRepository(ManageableContainer<?> root) {
		this.resourceRoot = root;
		this.repositoryRoot = new RepositoryEntry(this, root, null, "/");
		DefinedTypeResolverFactory.getInstance().addResolver(new EAIRepositoryTypeResolver(this));
		DefinedTypeResolverFactory.getInstance().addResolver(new SPIDefinedTypeResolver());
		DefinedTypeResolverFactory.getInstance().addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
		DefinedServiceResolverFactory.getInstance().addResolver(new EAIRepositoryServiceResolver(this));
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

	private void load(RepositoryEntry entry) {
		for (RepositoryEntry child : entry) {
			if (child.isNode()) {
				buildReferenceMap(child.getId(), child.getNode().getReferences());
				Class<? extends Artifact> artifactClass = child.getNode().getArtifactClass();
				if (!nodesByType.containsKey(artifactClass)) {
					nodesByType.put(artifactClass, new HashMap<String, Node>());
				}
				nodesByType.get(artifactClass).put(child.getId(), child.getNode());
				nodes.put(child.getId(), child.getNode());
			}
			else if (!child.isLeaf()) {
				load(child);
			}
		}
	}

	@Override
	public Artifact resolve(String id) {
		Node node = nodes.get(id);
		try {
			return node != null 
				? node.getArtifact()
				: null;
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
}

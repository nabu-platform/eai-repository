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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.events.RepositoryEvent;
import be.nabu.eai.repository.events.RepositoryEvent.RepositoryState;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
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
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.services.pojo.POJOInterfaceResolver;
import be.nabu.libs.types.DefinedSimpleTypeResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.ParsedPath;
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
 * TODO: add a node event listener for rename, move, delete etc node events to update reference?
 * 		> currently they are static
 */
public class EAIResourceRepository implements ResourceRepository {
	
	private ResourceContainer<?> resourceRoot;
	private RepositoryEntry repositoryRoot;
	private EventDispatcher dispatcher = new EventDispatcherImpl();
	private List<be.nabu.libs.maven.ResourceRepository> mavenRepositories = new ArrayList<be.nabu.libs.maven.ResourceRepository>();
	private List<String> internalDomains;
	
	private static EAIResourceRepository instance;
	
	public static final String PRIVATE = "private";
	public static final String PUBLIC = "public";
	
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
	
	public void updateReferences(String id, List<String> references) {
		unbuildReferenceMap(id);
		buildReferenceMap(id, references);
	}
	
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
	
	private void unbuildReferenceMap(String id) {
		this.references.remove(id);
		for (String dependency : dependencies.keySet()) {
			if (dependencies.get(dependency).contains(id)) {
				dependencies.get(dependency).remove(id);
			}
		}
	}
	
	public List<String> getDependencies(String id) {
		return dependencies.containsKey(id) ? dependencies.get(id) : new ArrayList<String>();
	}
	
	public List<String> getReferences(String id) {
		return references.containsKey(id) ? references.get(id) : new ArrayList<String>();
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
		reset();
		if (entry.isNode()) {
			unbuildReferenceMap(entry.getId());
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
	
	public void reload(String id) {
		reload(id, true);
	}

	private void reload(String id, boolean recursiveReload) {
		logger.info("Reloading: " + id);
		Entry entry = getEntry(id);
		while (entry == null && id.contains(".")) {
			int index = id.lastIndexOf('.');
			id = id.substring(0, index);
			entry = getEntry(id);
		}
		if (entry != null) {
			unload(entry);
			load(entry);
			// also reload all the dependencies
			// prevent concurrent modification
			if (recursiveReload) {
				List<String> dependenciesToReload = calculateDependenciesToReload(entry.getId());
				for (String dependency : dependenciesToReload) {
					reload(dependency, false);
				}
			}
		}
		if (recursiveReload) {
			reattachMavenArtifacts();
		}
	}
	
	private List<String> calculateDependenciesToReload(String id) {
		List<String> directDependencies = getDependencies(id);
		List<String> dependenciesToReload = new ArrayList<String>(directDependencies);
		for (String directDependency : directDependencies) {
			List<String> indirectDependencies = calculateDependenciesToReload(directDependency);
			// remove any dependencies that are also in the indirect ones
			// we can add them again afterwards which means they will only be in the list once and in the correct order
			dependenciesToReload.removeAll(indirectDependencies);
			dependenciesToReload.addAll(indirectDependencies);
		}
		return dependenciesToReload;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void load(Entry entry) {
		// refresh every entry before reloading it, there could be new elements (e.g. remote changes to repo)
		entry.refresh();
		// reset this to make sure any newly loaded entries are picked up or old entries are deleted
		reset();
		if (entry.isNode()) {
			logger.info("Loading entry: " + entry.getId());
			buildReferenceMap(entry.getId(), entry.getNode().getReferences());
			if (entry instanceof ModifiableEntry && entry.isNode() && entry.getNode().getArtifactManager() != null && ArtifactRepositoryManager.class.isAssignableFrom(entry.getNode().getArtifactManager())) {
				logger.debug("Loading children of: " + entry.getId());
				try {
					List<Entry> addedChildren = ((ArtifactRepositoryManager) entry.getNode().getArtifactManager().newInstance()).addChildren((ModifiableEntry) entry, entry.getNode().getArtifact());
					if (addedChildren != null) {
						for (Entry addedChild : addedChildren) {
							buildReferenceMap(addedChild.getId(), addedChild.getNode().getReferences());
						}
					}
				}
				catch(Exception e) {
					logger.error("Could not finish loading generated children for: " + entry.getId(), e);
				}
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
		if (!isValidName(parent.getContainer(), targetName)) {
			throw new IOException("The name is not valid: " + targetName);
		}
		ResourceEntry entry = (ResourceEntry) sourceEntry;
		// copy the contents to the new location
		ResourceUtils.copy(entry.getContainer(), (ManageableContainer<?>) parent.getContainer(), targetName);
		// we need to refresh the parent entry as it can cache the children and not see the new addition
		targetParent.refresh();
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
			relink(entry, newEntry, validations);
			// unload the node
			unload(entry.getId());
			// delete the original contents
			((ManageableContainer<?>) entry.getContainer().getParent()).delete(entry.getName());
		}
		scanForTypes();
		return validations;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void relink(Entry from, Entry to, List<Validation<?>> validations) {
		List<String> dependencies = new ArrayList<String>(getDependencies(from.getId()));
		if (dependencies != null) {
			for (String dependency : dependencies) {
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
							reload(dependency);
						}
						catch (Exception e) {
							logger.error("Could not update reference for dependency '" + dependency + "' from '" + from.getId() + "' to '" + to.getId() + "'");
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
					relink(child, target, validations);
				}
			}
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
		if (parent.getChild(name) != null) {
			return false;
		}
		return name.matches("^[a-z]+[\\w]+$") && !RESERVED.contains(name);
	}
	
	public void unloadMavenArtifact(be.nabu.libs.maven.api.Artifact artifact) throws IOException {
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
					mavenManager.removeChildren(getRoot(), mavenArtifact);
					iterator.remove();
				}
			}
		}
	}
	
	public void loadMavenArtifact(be.nabu.libs.maven.api.Artifact artifact) {
		if (mavenManager.getRepository().isInternal(artifact)) {
			startMavenArtifact(mavenManager, artifact);
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
				startMavenArtifact(mavenManager, internal);
			}
		}
		catch (IOException e) {
			logger.error("Could not load artifacts from maven repository", e);
		}
	}

	private void startMavenArtifact(MavenManager mavenManager, be.nabu.libs.maven.api.Artifact internal) {
		try {
			logger.info("Loading maven artifact " + internal.getGroupId() + " > " + internal.getArtifactId());
			MavenArtifact artifact = mavenManager.load(internal, localMavenServer, updateMavenSnapshots);
			mavenArtifacts.add(artifact);
			mavenManager.removeChildren(getRoot(), artifact);
			mavenManager.addChildren(getRoot(), artifact);
			mavenIfaceResolvers.put(artifact, new POJOInterfaceResolver(artifact.getClassLoader()));
			DefinedServiceInterfaceResolverFactory.getInstance().addResolver(mavenIfaceResolvers.get(artifact));
		}
		catch (IOException e) {
			logger.error("Could not load artifact: " + internal.getGroupId() + " > " + internal.getArtifactId(), e);
		}
	}
	
	private void reattachMavenArtifacts() {
		for (MavenArtifact artifact : mavenArtifacts) {
			try {
				MavenManager.attachChildren(getRoot(), artifact);
			}
			catch (IOException e) {
				logger.error("Could not reattach maven artifact: " + artifact, e);
			}
		}
	}
	
	public be.nabu.libs.maven.api.DomainRepository getMavenRepository() {
		return mavenManager != null ? mavenManager.getRepository() : null;
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
		return new EAIExecutionContext(this, principal, isDevelopment());
	}

	@SuppressWarnings("unchecked")
	public <T extends Artifact> List<T> getArtifacts(Class<T> artifactClazz) {
		List<T> artifacts = new ArrayList<T>();
		for (Node node : getNodes(artifactClazz)) {
			try {
				artifacts.add((T) node.getArtifact());
			}
			catch (Exception e) {
				logger.error("Could not load node: " + node);
			}
		}
		return artifacts;
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
		// the load can remove maven artifacts
		reattachMavenArtifacts();
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
	
	public List<Class<?>> getMavenImplementationsFor(Class<?> clazz) throws IOException {
		List<Class<?>> implementations = new ArrayList<Class<?>>();
		for (MavenArtifact artifact : mavenArtifacts) {
			List<Class<?>> provided = artifact.getImplementations().get(clazz);
			if (provided != null) {
				implementations.addAll(provided);
			}
		}
		return implementations;
	}
	
	public Class<?> getMavenClass(String className) throws IOException {
		for (MavenArtifact artifact : mavenArtifacts) {
			for (List<Class<?>> list : artifact.getImplementations().values()) {
				for (Class<?> clazz : list) {
					if (clazz.getName().equals(className)) {
						return clazz;
					}
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
}

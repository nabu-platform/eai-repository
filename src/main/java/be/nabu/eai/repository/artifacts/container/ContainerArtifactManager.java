package be.nabu.eai.repository.artifacts.container;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ContainerArtifact;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

@SuppressWarnings({ "rawtypes", "unchecked" })
abstract public class ContainerArtifactManager<T extends ContainerArtifact> implements ArtifactManager<T> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	abstract public T newInstance(String id);
	
	protected List<ResourceContainer<?>> getChildrenToLoad(ResourceContainer<?> directory) {
		List<ResourceContainer<?>> children = new ArrayList<ResourceContainer<?>>();
		for (Resource child : directory) {
			if (child instanceof ResourceContainer) {
				children.add((ResourceContainer) child);
			}
		}
		return children;
	}
	
	@Override
	public T load(final ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			T newInstance = newInstance(entry.getId());
			// also load the root container, there could be a "main" artifact
			List<ResourceContainer<?>> childrenToLoad = new ArrayList<ResourceContainer<?>>(getChildrenToLoad(privateDirectory));
			childrenToLoad.add(entry.getContainer());
			for (ResourceContainer childToLoad : childrenToLoad) {
				// not all artifacts are required
				if (childToLoad == null) {
					continue;
				}
				try {
					ReadableResource containerConfig = (ReadableResource) childToLoad.getChild("container.xml");
					if (containerConfig != null) {
						ContainerArtifactConfiguration configuration;
						JAXBContext context = JAXBContext.newInstance(ContainerArtifactConfiguration.class);
						ReadableContainer<ByteBuffer> readable = containerConfig.getReadable();
						try {
							configuration = (ContainerArtifactConfiguration) context.createUnmarshaller().unmarshal(IOUtils.toInputStream(readable));
						}
						finally {
							readable.close();
						}
						String name = entry.getContainer().equals(childToLoad) ? "main" : childToLoad.getName();
						ArtifactManager artifactManager = (ArtifactManager) configuration.getArtifactManagerClass().newInstance();
						// we already need repository-based resolving because of remote repositories so we reuse that here to dynamically add the already loaded artifacts to be visible for resolving
						Artifact artifact = artifactManager.load(new WrapperEntry(new ContainerRepository(entry.getId(), (RepositoryEntry) entry, newInstance.getContainedArtifacts()), entry, childToLoad, name), messages);
						if (artifact != null) {
							newInstance.addArtifact(name, artifact, configuration.getConfiguration());
						}
					}
				}
				catch (Exception e) {
					logger.error("Could not load child: " + childToLoad, e);
					throw new RuntimeException(e);
				}
			}
			return newInstance;
		}
		return null;
	}

	@Override
	public List<Validation<?>> save(final ResourceEntry entry, T artifact) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory == null) {
			privateDirectory = ResourceUtils.mkdirs(entry.getContainer(), EAIResourceRepository.PRIVATE);
		}
		// at the end we need to delete unused directories, it could be that an artifact was removed from the container
		List<String> unused = new ArrayList<String>();
		for (Resource resource : privateDirectory) {
			if (resource instanceof ResourceContainer) {
				unused.add(resource.getName());
			}
		}
		for (Artifact child : artifact.getContainedArtifacts()) {
			String name = artifact.getPartName(child);
			ResourceContainer<?> target = name.equals("main") ? entry.getContainer() : (ResourceContainer<?>) privateDirectory.getChild(name);
			if (target == null) {
				target = ResourceUtils.mkdirs(privateDirectory, name);	
			}
			unused.remove(name);
			ContainerArtifactConfiguration container = new ContainerArtifactConfiguration();
			ArtifactManager artifactManager = entry.getRepository().getArtifactManager(child.getClass());
			if (artifactManager == null) {
				throw new RuntimeException("Can not save artifact " + child + ", no manager found");
			}
			List<Validation<?>> childMessages = artifactManager.save(new WrapperEntry(entry.getRepository(), entry, target, name), child);
			if (childMessages != null) {
				messages.addAll(childMessages);
			}
			container.setArtifactManagerClass(artifactManager.getClass());
			container.setConfiguration(artifact.getConfiguration(child));
			try {
				JAXBContext context = JAXBContext.newInstance(ContainerArtifactConfiguration.class);
				WritableResource resource = (WritableResource) target.getChild("container.xml");
				if (resource == null) {
					resource = (WritableResource) ((ManageableContainer<?>) target).create("container.xml", "application/xml");
				}
				WritableContainer<ByteBuffer> writable = resource.getWritable();
				try {
					context.createMarshaller().marshal(container, IOUtils.toOutputStream(writable));
				}
				finally {
					writable.close();
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		for (String toDelete : unused) {
			((ManageableContainer<?>) privateDirectory).delete(toDelete);
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return messages;
	}
	
	public static class WrapperEntry implements ResourceEntry {

		private ResourceRepository repository;
		private ResourceContainer target;
		private Entry parent;
		private String name;

		public WrapperEntry(ResourceRepository repository, Entry parent, ResourceContainer target, String name) {
			this.repository = repository;
			this.parent = parent;
			this.target = target;
			this.name = name;
		}
		
		@Override
		public String getId() {
			return "$self:" + getName();
		}

		@Override
		public boolean isLeaf() {
			return true;
		}

		@Override
		public boolean isEditable() {
			return true;
		}

		@Override
		public boolean isNode() {
			return false;
		}

		@Override
		public Node getNode() {
			return null;
		}

		@Override
		public void refresh(boolean recursive) {
			// do nothing
		}

		@Override
		public Entry getParent() {
			return parent;
		}

		@Override
		public Entry getChild(String name) {
			return null;
		}

		@Override
		public String getContentType() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Iterator<Entry> iterator() {
			return null;
		}

		@Override
		public ResourceRepository getRepository() {
			return repository;
		}

		@Override
		public ResourceContainer<?> getContainer() {
			return target;
		}
		
	}

	@Override
	public List<String> getReferences(T artifact) throws IOException {
		Set<String> references = new HashSet<String>();
		for (Artifact child : artifact.getContainedArtifacts()) {
			ArtifactManager artifactManager = EAIResourceRepository.getInstance().getArtifactManager(child.getClass());
			if (artifactManager != null) {
				references.addAll(artifactManager.getReferences(child));
			}
		}
		return new ArrayList<String>(references);
	}

	@Override
	public List<Validation<?>> updateReference(T artifact, String from, String to) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		for (Artifact child : artifact.getContainedArtifacts()) {
			ArtifactManager artifactManager = EAIResourceRepository.getInstance().getArtifactManager(child.getClass());
			if (artifactManager != null) {
				List<Validation<?>> updateReference = artifactManager.updateReference(child, from, to);
				if (updateReference != null) {
					messages.addAll(updateReference);
				}
			}
		}
		return messages;
	}

	public static class ContainerRepository implements ResourceRepository {
		
		private String id;
		private RepositoryEntry parent;
		private Collection<Artifact> additionalArtifacts;
		private Map<String, String> aliasedArtifacts = new HashMap<String, String>();

		public ContainerRepository(String id, RepositoryEntry parent, Collection<Artifact> additionalArtifacts) {
			this.id = id;
			this.parent = parent;
			this.additionalArtifacts = additionalArtifacts;
		}

		@Override
		public Entry getEntry(String id) {
			return parent.getRepository().getEntry(id);
		}

		@Override
		public Charset getCharset() {
			return parent.getRepository().getCharset();
		}

		@Override
		public EventDispatcher getEventDispatcher() {
			return parent.getRepository().getEventDispatcher();
		}

		@Override
		public Node getNode(String id) {
			return parent.getRepository().getNode(id);
		}

		@Override
		public void reload(String id) {
			parent.getRepository().reload(id);
		}

		@Override
		public void reloadAll() {
			parent.getRepository().reloadAll();
		}

		@Override
		public void unload(String id) {
			parent.getRepository().unload(id);
		}

		@Override
		public List<Node> getNodes(Class<? extends Artifact> artifactClazz) {
			return parent.getRepository().getNodes(artifactClazz);
		}

		@Override
		public ServiceRunner getServiceRunner() {
			return parent.getRepository().getServiceRunner();
		}

		@Override
		public void setServiceRunner(ServiceRunner serviceRunner) {
			parent.getRepository().setServiceRunner(serviceRunner);
		}

		@Override
		public List<String> getReferences(String id) {
			// TODO: add inline artifacts?
			return parent.getRepository().getReferences(id);
		}

		@Override
		public List<String> getDependencies(String id) {
			// TODO: add inline artifacts?
			return parent.getRepository().getDependencies(id);
		}

		@Override
		public void start() {
			parent.getRepository().start();
		}

		@Override
		public ClassLoader newClassLoader(String artifact) {
			return parent.getRepository().newClassLoader(artifact);
		}

		@Override
		public <T> List<Class<T>> getImplementationsFor(Class<T> clazz) {
			return parent.getRepository().getImplementationsFor(clazz);
		}

		@Override
		public <T extends Artifact> List<T> getArtifacts(Class<T> artifactClazz) {
			List<T> artifacts = new ArrayList<T>(parent.getRepository().getArtifacts(artifactClazz));
			for (Artifact artifact : additionalArtifacts) {
				if (artifactClazz.isAssignableFrom(artifact.getClass())) {
					artifacts.add((T) artifact);
				}
			}
			return artifacts;
		}

		@Override
		public <T extends Artifact> ArtifactManager<T> getArtifactManager(Class<T> artifactClass) {
			return parent.getRepository().getArtifactManager(artifactClass);
		}

		@Override
		public Artifact resolve(String id) {
			if (aliasedArtifacts.containsKey(id)) {
				return resolve(aliasedArtifacts.get(id));
			}
			else if (id.contains(":") && id.split(":")[0].equals(this.id)) {
				for (Artifact artifact : additionalArtifacts) {
					if (artifact.getId().endsWith(id.substring(this.id.length()))) {
						return artifact;
					}
				}
			}
			else if (id.startsWith("$self:")) {
				for (Artifact artifact : additionalArtifacts) {
					if (artifact.getId().endsWith(id.substring("$self".length()))) {
						return artifact;
					}
				}
			}
			return parent.getRepository().resolve(id);
		}

		@Override
		public ExecutionContext newExecutionContext(Principal principal) {
			return parent.getRepository().newExecutionContext(principal);
		}

		@Override
		public List<DefinedService> getServices() {
			List<DefinedService> services = new ArrayList<DefinedService>(parent.getRepository().getServices());
			for (Artifact artifact : additionalArtifacts) {
				if (artifact instanceof DefinedService) {
					services.add((DefinedService) artifact);
				}
			}
			return services;
		}

		@Override
		public MetricInstance getMetricInstance(String id) {
			return parent.getRepository().getMetricInstance(id);
		}

		@Override
		public ResourceEntry getRoot() {
			return parent.getRepository().getRoot();
		}

		@Override
		public boolean isInternal(ResourceContainer<?> container) {
			return parent.getRepository().isInternal(container);
		}

		@Override
		public boolean isValidName(ResourceContainer<?> parent, String name) {
			return this.parent.getRepository().isValidName(parent, name);
		}
		
		public void alias(String original, String newName) {
			aliasedArtifacts.put(newName, original);
		}

		@Override
		public void reloadAll(Collection<String> ids) {
			this.parent.getRepository().reloadAll(ids);
		}

		@Override
		public <T> List<T> getArtifactsThatImplement(Class<T> ifaceClass) {
			return this.parent.getRepository().getArtifactsThatImplement(ifaceClass);
		}
	}
}

package be.nabu.eai.repository.managers.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.validator.api.Validation;

abstract public class TypeRegistryManager<T extends TypeRegistry & Artifact> implements ArtifactRepositoryManager<T> {

	private Class<T> clazz;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public TypeRegistryManager(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public List<Validation<?>> save(ResourceEntry entry, T artifact) throws IOException {
		return null;
	}

	@Override
	public Class<T> getArtifactClass() {
		return clazz;
	}

	@Override
	public List<String> getReferences(T artifact) throws IOException {
		return null;
	}

	@Override
	public List<Validation<?>> updateReference(T artifact, String from, String to) throws IOException {
		return null;
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry root, T artifact) throws IOException {
		((EAINode) root.getNode()).setLeaf(false);
		List<Entry> entries = new ArrayList<Entry>();
		for (String namespace : artifact.getNamespaces()) {
			// currently we only expose the complex types
			for (ComplexType type : artifact.getComplexTypes(namespace)) {
				logger.debug("Loading complex type: " + type + " in namespace: " + namespace);
				// only exposed the defined complex types, not anonymous ones
				if (type instanceof DefinedType) {
					String id = ((DefinedType) type).getId();
					if (id.startsWith(artifact.getId())) {
						id = id.substring((artifact.getId() + ".").length());
						String name = id.replaceAll("^.*\\.([^.]+)$", "$1");
						ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id, false);
						System.out.println("Adding " + name + " to " + parent.getId() + " / " + parent);
						EAINode node = new EAINode();
						node.setArtifact((DefinedType) type);
						node.setLeaf(true);
						MemoryEntry child = new MemoryEntry(artifact.getId(), root.getRepository(), parent, node, parent.getId() + "." + name, name);
						node.setEntry(child);
						parent.addChildren(child);
						entries.add(child);
					}
				}
			}
			for (SimpleType<?> type : artifact.getSimpleTypes(namespace)) {
				logger.debug("Loading complex type: " + type + " in namespace: " + namespace);
				// only exposed the defined complex types, not anonymous ones
				if (type instanceof DefinedType) {
					String id = ((DefinedType) type).getId();
					if (id.startsWith(artifact.getId())) {
						id = id.substring((artifact.getId() + ".").length());
						String name = id.replaceAll("^.*\\.([^.]+)$", "$1");
						ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id, false);
						System.out.println("Adding " + name + " to " + parent.getId() + " / " + parent);
						EAINode node = new EAINode();
						node.setArtifact((DefinedType) type);
						node.setLeaf(true);
						MemoryEntry child = new MemoryEntry(artifact.getId(), root.getRepository(), parent, node, parent.getId() + "." + name, name);
						node.setEntry(child);
						parent.addChildren(child);
						entries.add(child);
					}
				}
			}
		}
		return entries;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry root, T artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		for (String namespace : artifact.getNamespaces()) {
			for (ComplexType type : artifact.getComplexTypes(namespace)) {
				if (type instanceof DefinedType) {
					String id = ((DefinedType) type).getId();
					if (id.startsWith(artifact.getId())) {
						id = id.substring((artifact.getId() + ".").length());
						String name = id.replaceAll("^.*\\.([^.]+)$", "$1");
						ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id, false);
						System.out.println("Removing " + name + " from " + parent.getId() + " / " + parent);
						if (parent != null) {
							entries.add(parent.getChild(name));
							parent.removeChildren(name);
							// if no children are remaining, remove parent as well
							if (!parent.iterator().hasNext()) {
								if (parent.getParent() instanceof ModifiableEntry) {
									entries.add(parent);
									((ModifiableEntry) parent.getParent()).removeChildren(parent.getName());
								}
							}
						}
					}
				}
			}
		}
		return entries;
	}
}

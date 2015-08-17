package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.validator.api.Validation;

abstract public class TypeRegistryManager<T extends TypeRegistry & Artifact> implements ArtifactRepositoryManager<T> {

	private Class<T> clazz;

	TypeRegistryManager(Class<T> clazz) {
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
				System.out.println("Loading complex type: " + type + " in namespace: " + namespace);
				// only exposed the defined complex types, not anonymous ones
				if (type instanceof DefinedType) {
					String id = ((DefinedType) type).getId();
					if (id.startsWith(artifact.getId())) {
						id = id.substring((artifact.getId() + ".").length());
						String name = id.replaceAll("^.*\\.([^.]+)$", "$1");
						ModifiableEntry parent = MavenManager.getParent(root, id, false);
						EAINode node = new EAINode();
						node.setArtifact((DefinedType) type);
						node.setLeaf(true);
						MemoryEntry child = new MemoryEntry(root.getRepository(), parent, node, parent.getId() + "." + name, name);
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
						ModifiableEntry parent = MavenManager.getParent(root, id, false);
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

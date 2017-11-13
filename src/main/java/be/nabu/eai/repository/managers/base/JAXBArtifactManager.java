package be.nabu.eai.repository.managers.base;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

abstract public class JAXBArtifactManager<C, T extends JAXBArtifact<C>> implements ArtifactManager<T> {

	private Class<T> artifactClass;

	public JAXBArtifactManager(Class<T> artifactClass) {
		this.artifactClass = artifactClass;
	}
	
	abstract protected T newInstance(String id, ResourceContainer<?> container, Repository repository);

	@Override
	public List<String> getReferences(T artifact) throws IOException {
		return getObjectReferences(artifact);
	}

	@Override
	public List<Validation<?>> updateReference(T artifact, String from, String to) throws IOException {
		return updateObjectReferences(artifact, from, to);
	}

	@Override
	public T load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		return newInstance(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, T artifact) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		try {
			artifact.save(entry.getContainer());
		}
		catch (IOException e) {
			messages.add(new ValidationMessage(Severity.ERROR, "Could not save " + artifactClass.getSimpleName() + ": " + e.getMessage()));
			return messages;
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return messages;
	}

	@Override
	public Class<T> getArtifactClass() {
		return artifactClass;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getObjectReferences(JAXBArtifact<?> artifact) throws IOException {
		return getObjectReferences(ComplexContentWrapperFactory.getInstance().getWrapper().wrap(artifact.getConfiguration()));
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> getObjectReferences(ComplexContent content) throws IOException {
		List<String> references = new ArrayList<String>();
		for (Element<?> child : TypeUtils.getAllChildren(content.getType())) {
			if (child.getType() instanceof SimpleType) {
				Class<?> instanceClass = ((SimpleType<?>) child.getType()).getInstanceClass();
				if (Artifact.class.isAssignableFrom(instanceClass)) {
					if (child.getType().isList(child.getProperties())) {
						List<Artifact> referencedArtifacts = (List<Artifact>) content.get(child.getName());
						if (referencedArtifacts != null) {
							for (Artifact listEntry : referencedArtifacts) {
								references.add(listEntry.getId());
							}
						}
					}
					else {
						Artifact referencedArtifact = (Artifact) content.get(child.getName());
						if (referencedArtifact != null) {
							references.add(referencedArtifact.getId());
						}
					}
				}
			}
		}
		return references;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Validation<?>> updateObjectReferences(JAXBArtifact<?> artifact, String from, String to) throws IOException {
		return updateObjectReferences(ComplexContentWrapperFactory.getInstance().getWrapper().wrap(artifact.getConfiguration()), artifact.getRepository(), from, to);
	}
	
	@SuppressWarnings("unchecked")
	public static List<Validation<?>> updateObjectReferences(ComplexContent content, Repository repository, String from, String to) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		for (Element<?> child : TypeUtils.getAllChildren(content.getType())) {
			if (child.getType() instanceof SimpleType) {
				Class<?> instanceClass = ((SimpleType<?>) child.getType()).getInstanceClass();
				if (Artifact.class.isAssignableFrom(instanceClass)) {
					if (child.getType().isList(child.getProperties())) {
						List<Artifact> referencedArtifacts = (List<Artifact>) content.get(child.getName());
						if (referencedArtifacts != null) {
							Artifact resolved = repository.resolve(to);
							if (resolved == null) {
								messages.add(new ValidationMessage(Severity.ERROR, "Could not find artifact '" + to + "', references not updated"));
							}
							else {
								for (int i = 0; i < referencedArtifacts.size(); i++) {
									if (referencedArtifacts.get(i).getId().equals(from)) {
										referencedArtifacts.set(i, resolved);
									}
								}
							}
						}
					}
					else {
						Artifact referencedArtifact = (Artifact) content.get(child.getName());
						if (referencedArtifact != null && referencedArtifact.getId().equals(from)) {
							Artifact resolved = repository.resolve(to);
							if (resolved == null) {
								messages.add(new ValidationMessage(Severity.ERROR, "Could not find artifact '" + to + "', references not updated"));
							}
							else {
								content.set(child.getName(), resolved);
							}
						}
					}
				}
			}
		}
		return messages;
	}
}

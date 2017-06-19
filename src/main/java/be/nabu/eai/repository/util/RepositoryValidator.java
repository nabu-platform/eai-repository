package be.nabu.eai.repository.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ValidatableArtifactManager;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class RepositoryValidator {

	private Repository repository;
	private Date lastChecked;
	
	public RepositoryValidator(Repository repository) {
		this.repository = repository;
	}

	private boolean shouldCheck(ResourceContainer<?> container) {
		for (Resource resource : container) {
			if (resource instanceof TimestampedResource) {
				Date lastModified = ((TimestampedResource) resource).getLastModified();
				if (lastModified != null && lastModified.after(lastChecked)) {
					return true;
				}
			}
			// we don't know when it was last changed...
			else {
				return true;
			}
			if (resource instanceof ResourceContainer) {
				boolean shouldCheck = shouldCheck((ResourceContainer<?>) resource);
				if (shouldCheck) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void scan(Entry entry, Map<Class<?>, List<Entry>> nodes) {
		if (entry.isNode()) {
			boolean shouldCheck = lastChecked == null;
			if (!shouldCheck && entry instanceof ResourceEntry) {
				shouldCheck = shouldCheck(((ResourceEntry) entry).getContainer());
			}
			if (shouldCheck) {
				Class<? extends Artifact> artifactClass = entry.getNode().getArtifactClass();
				if (!nodes.containsKey(artifactClass)) {
					nodes.put(artifactClass, new ArrayList<Entry>());
				}
				nodes.get(artifactClass).add(entry);
			}
		}
		for (Entry child : entry) {
			scan(child, nodes);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<Entry, List<? extends Validation<?>>> validate() {
		Map<Class<?>, List<Entry>> nodes = new HashMap<Class<?>, List<Entry>>();
		scan(repository.getRoot(), nodes);
		lastChecked = new Date();
		Map<Entry, List<? extends Validation<?>>> result = new HashMap<Entry, List<? extends Validation<?>>>();
		for (Class clazz : nodes.keySet()) {
			ArtifactManager manager = EAIRepositoryUtils.getArtifactManager(clazz);
			for (Entry entry : nodes.get(clazz)) {
				List<Validation<?>> validations = new ArrayList<Validation<?>>();
				try {
					// check all the references
					for (String reference : repository.getReferences(entry.getId())) {
						if (reference != null && EAIRepositoryUtils.isBrokenReference(repository, reference)) {
							validations.add(new ValidationMessage(Severity.ERROR, "Broken reference: " + reference));
						}
					}
					
					Artifact artifact = entry.getNode().getArtifact();
					if (manager instanceof ValidatableArtifactManager) {
						List<? extends Validation<?>> artifactValidations = ((ValidatableArtifactManager) manager).validate(artifact);
						if (artifactValidations != null && !artifactValidations.isEmpty()) {
							validations.addAll(artifactValidations);
						}
					}
				}
				catch (IOException e) {
					validations.add(new ValidationMessage(Severity.ERROR, "Could not open the artifact", 0, e.getMessage()));
				}
				catch (ParseException e) {
					validations.add(new ValidationMessage(Severity.ERROR, "Could not open the artifact", 1, e.getMessage()));
				}
				if (!validations.isEmpty()) {
					result.put(entry, validations);
				}
			}
		}
		return result;
	}

}

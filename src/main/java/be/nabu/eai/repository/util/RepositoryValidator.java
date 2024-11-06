/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ValidatableArtifactManager;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.memory.MemoryResource;
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
		if (entry.isNode() && entry instanceof ResourceEntry) {
			// we only want to validate things that are useful, in other words, things you can _fix_
			// if you can't actually write to the node container, it doesn't matter that there are errors
			// the quickest way to check this is to see if the node.xml is writable
			Resource child = ((ResourceEntry) entry).getContainer().getChild("node.xml");
			// if we have memory resources, they are writable but ineffective...
			if (child instanceof WritableResource && !(child instanceof MemoryResource)) {
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
					// we are not interested in validation errors from already deprecated stuff, we won't be fixing it
					if (entry.isNode()) {
						Node node = entry.getNode();
						if (node instanceof EAINode && ((EAINode) node).getDeprecated() != null && ((EAINode) node).getDeprecated().before(new Date())) {
							continue;
						}
					}
				}
				catch (Exception e) {
					validations.add(new ValidationMessage(Severity.ERROR, "Could not load artifact '" + entry.getId() + "': " + e.getMessage()));
				}
				try {
					// check all the references
					for (String reference : repository.getReferences(entry.getId())) {
						// just...don't
						if (reference != null && reference.equals("[B")) {
							continue;
						}
						if (reference != null && EAIRepositoryUtils.isBrokenReference(repository, reference)) {
							validations.add(new ValidationMessage(Severity.ERROR, "Broken reference: " + reference));
						}
						else if (reference != null) {
							try {
								Entry referenceEntry = repository.getEntry(reference);
								if (referenceEntry != null && referenceEntry.isNode()) {
									Node node = referenceEntry.getNode();
									// if deprecated, we flag it
									if (node instanceof EAINode && ((EAINode) node).getDeprecated() != null && ((EAINode) node).getDeprecated().before(new Date())) {
										validations.add(new ValidationMessage(Severity.ERROR, "Deprecated reference: " + reference));
									}
								}
							}
							catch (Exception e) {
								validations.add(new ValidationMessage(Severity.ERROR, "Could not load reference '" + reference + "': " + e.getMessage()));
							}
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
					validations.add(new ValidationMessage(Severity.ERROR, "Could not open the artifact", "REPOSITORY-0", e.getMessage()));
				}
				catch (ParseException e) {
					validations.add(new ValidationMessage(Severity.ERROR, "Could not open the artifact", "REPOSITORY-1", e.getMessage()));
				}
				if (!validations.isEmpty()) {
					result.put(entry, validations);
				}
			}
		}
		return result;
	}

}

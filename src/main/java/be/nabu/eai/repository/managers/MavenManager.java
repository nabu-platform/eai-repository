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

package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.api.Cache;
import be.nabu.eai.api.Eager;
import be.nabu.eai.api.Hidden;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.events.NodeEvent;
import be.nabu.eai.repository.events.NodeEvent.State;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.eai.repository.util.NodeUtils;
import be.nabu.libs.maven.api.Artifact;
import be.nabu.libs.maven.api.DomainRepository;
import be.nabu.libs.services.api.ServiceDescription;
import be.nabu.libs.services.maven.DependencyResolver;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.types.api.DefinedTypeResolver;
import be.nabu.libs.validator.api.Validation;

public class MavenManager implements ArtifactRepositoryManager<MavenArtifact> {
	
	private DefinedTypeResolver definedTypeResolver;
	
	/**
	 * @2025-06-17
	 * Due to changes in java packaging, some core libraries that were originally present (e.g. jaxb) were booted from the default distributions
	 * This necessitates adding them as dependencies to maven to be able to build
	 * However, this cascades down and "infects" a lot of libraries with additional dependencies. This is centrally provided however by developer/server and I don't want to retrofit all poms (or library artifacts) to explicitly ignore these libraries
	 * So I added a global ignore list
	 */
	private static List<String> artifactsToIgnore = Arrays.asList("javax.xml.bind:jaxb-api", "com.sun.xml.bind:jaxb-impl", "com.sun.xml.bind:jaxb-core", "javax.activation:activation", "javax.jws:javax.jws-api",
			"org.openjfx:javafx-swing");
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	private DomainRepository repository;

	public MavenManager(DomainRepository repository, DefinedTypeResolver definedTypeResolver) {
		this.repository = repository;
		this.definedTypeResolver = definedTypeResolver;	
	}
	
	public MavenArtifact load(Repository repository, Artifact artifact, boolean updateSnapshots, URI...mavenServer) {
		try {
			DependencyResolver dependencyResolver = getResolver(updateSnapshots, mavenServer);
			String id = artifact.getGroupId() + "." + artifact.getArtifactId();
			MavenArtifact mavenArtifact = new MavenArtifact(
				repository.getClassLoader(),
				definedTypeResolver, 
				dependencyResolver, 
				this.repository, 
				id, 
				artifact
			);
			return mavenArtifact;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private DependencyResolver getResolver(boolean updateSnapshots, URI...mavenServer) throws URISyntaxException {
		List<URI> endpoints = new ArrayList<URI>();
		if (mavenServer != null) {
			for (URI uri : mavenServer) {
				endpoints.add(uri);
			}
		}
		endpoints.add(new URI("http://central.maven.org/maven2"));
		endpoints.add(new URI("http://mirrors.ibiblio.org/maven2"));
		DependencyResolver dependencyResolver = new DependencyResolver(endpoints.toArray(new URI[endpoints.size()]));
		dependencyResolver.setUpdateSnapshots(updateSnapshots);
		dependencyResolver.setArtifactsToIgnore(artifactsToIgnore);
		return dependencyResolver;
	}
	
	@Override
	public MavenArtifact load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		String id = entry.getId();
		int index = id.lastIndexOf('.');
		if (index < 0) {
			throw new IOException("Can't find a maven repository without both group & artifact id");
		}
		String groupId = id.substring(0, index);
		String artifactId = id.substring(index + 1);
		List<String> versions = new ArrayList<String>(repository.getVersions(groupId, artifactId));
		if (versions.isEmpty()) {
			throw new IOException("Can not find the artifact " + id);
		}
		be.nabu.libs.maven.api.Artifact mavenArtifact = repository.getArtifact(groupId, artifactId, versions.get(versions.size() - 1), false);
		if (mavenArtifact == null) {
			throw new IOException("Can not find the artifact " + id);
		}
		try {
			return new MavenArtifact(entry.getRepository().getClassLoader(), definedTypeResolver, getResolver(false), repository, id, mavenArtifact);
		}
		catch (URISyntaxException e) {
			logger.error("Could not load: " + id, e);
			throw new IOException(e);
		}
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, MavenArtifact artifact) throws IOException {
		throw new IOException("Can not update a maven repository this way, please use the maven endpoint");
	}

	@Override
	public Class<MavenArtifact> getArtifactClass() {
		return MavenArtifact.class;
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry root, MavenArtifact artifact) throws IOException {
		return attachChildren(root, artifact);
	}

	public static List<Entry> attachChildren(ModifiableEntry root, MavenArtifact artifact) throws IOException {
		return attachChildren(root, artifact, root.getId());
	}
	
	public static List<Entry> attachChildren(ModifiableEntry root, MavenArtifact artifact, String parentId) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		List<String> keys = new ArrayList<String>(artifact.getChildren().keySet());
		if (!keys.isEmpty()) {
			// if you are adding it to the actual repository root, first create an entry for the groupId
			// this allows you to define maven repositories in other places than the groupId
			if (root.getParent() == null) {
				root = EAIRepositoryUtils.getParent(root, artifact.getArtifact().getGroupId(), true);
			}
			Collections.sort(keys);
			
			for (String childId : keys) {
				ModifiableEntry parent = EAIRepositoryUtils.getParent(root, childId, false);
				int index = childId.lastIndexOf('.');
				String childName = index < 0 ? childId : childId.substring(index + 1);
				if (parent.getChild(childName) == null) {
					String entryId = parent.getId() + "." + childName;
					root.getRepository().getEventDispatcher().fire(new NodeEvent(entryId, null, State.LOAD, false), artifact);
					EAINode node = new EAINode();
					if (parentId != null && !parentId.equals("")) {
						node.getReferences().add(parentId);
					}
					node.setArtifact(artifact.getChildren().get(childId));
					boolean hidden = false;
					Annotation[] annotations = artifact.getAnnotations(childId);
					for (Annotation annotation : annotations) {
						if (annotation instanceof Cache) {
							Long timeout = ((Cache) annotation).timeout();
							Boolean refresh = ((Cache) annotation).refresh();
							node.getProperties().put(NodeUtils.CACHE_TIMEOUT, timeout.toString());
							node.getProperties().put(NodeUtils.CACHE_REFRESH, refresh.toString());
						}
						else if (annotation instanceof Eager) {
							node.getProperties().put(NodeUtils.LOAD_TYPE, "eager");
						}
						else if (annotation instanceof Hidden) {
							hidden = true;
						}
						else if (annotation instanceof ServiceDescription) {
							ServiceDescription description = (ServiceDescription) annotation;
							if (!description.description().trim().isEmpty()) {
								node.setDescription(description.description().trim());
							}
							if (!description.summary().trim().isEmpty()) {
								node.setSummary(description.summary().trim());
							}
							if (!description.name().trim().isEmpty()) {
								node.setName(description.name().trim());
							}
							if (!description.comment().trim().isEmpty()) {
								node.setComment(description.comment().trim());
							}
						}
					}
					node.setLeaf(true);
					MemoryEntry child = new MemoryEntry(parentId, root.getRepository(), parent, node, entryId, childName);
					node.setEntry(child);
		//			node.setEntry(parent);
//					if (!hidden) {
						parent.addChildren(child);
//					}
					node.setHidden(hidden);
					root.getRepository().getEventDispatcher().fire(new NodeEvent(entryId, node, State.LOAD, true), artifact);
					entries.add(child);
				}
			}
		}
		return entries;
	}
	
	@Override
	public List<Entry> removeChildren(ModifiableEntry root, MavenArtifact artifact) throws IOException {
		return detachChildren(root, artifact);
	}

	public static List<Entry> detachChildren(ModifiableEntry root, MavenArtifact artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		if (root.getParent() == null) {
			root = EAIRepositoryUtils.getParent(root, artifact.getArtifact().getGroupId(), true);
		}
		for (String id : artifact.getChildren().keySet()) {
			int index = id.lastIndexOf('.');
			ModifiableEntry parent = index < 0 ? root : EAIRepositoryUtils.getParent(root, id, false);
			String name = index < 0 ? id : id.substring(index + 1);
			if (parent.getChild(name) != null) {
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
		return entries;
	}

	@Override
	public List<String> getReferences(MavenArtifact artifact) throws IOException {
		return null;
	}

	@Override
	public List<Validation<?>> updateReference(MavenArtifact artifact, String from, String to) throws IOException {
		return null;
	}

	public DomainRepository getRepository() {
		return repository;
	}
}

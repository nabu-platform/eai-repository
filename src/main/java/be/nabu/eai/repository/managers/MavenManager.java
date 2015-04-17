package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.nabu.eai.api.Cache;
import be.nabu.eai.api.Eager;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.eai.repository.util.NodeUtils;
import be.nabu.libs.maven.api.Artifact;
import be.nabu.libs.maven.api.DomainRepository;
import be.nabu.libs.services.maven.DependencyResolver;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.api.DefinedTypeResolver;
import be.nabu.libs.validator.api.ValidationMessage;

public class MavenManager implements ArtifactRepositoryManager<MavenArtifact> {
	
	private DefinedTypeResolver definedTypeResolver;

	public MavenManager(DefinedTypeResolver definedTypeResolver) {
		this.definedTypeResolver = definedTypeResolver;	
	}
	
	public MavenArtifact load(DomainRepository repository, Artifact artifact, URI mavenServer, boolean updateSnapshots) {
		try {
			DependencyResolver dependencyResolver = mavenServer == null 
				? new DependencyResolver(new URI("http://mirrors.ibiblio.org/maven2"))
				: new DependencyResolver(mavenServer, new URI("http://mirrors.ibiblio.org/maven2"));
			dependencyResolver.setUpdateSnapshots(updateSnapshots);
			return new MavenArtifact(definedTypeResolver, dependencyResolver, repository, artifact.getGroupId() + "." + artifact.getArtifactId(), artifact);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public MavenArtifact load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		String id = entry.getId();
		int index = id.lastIndexOf('.');
		if (index < 0) {
			throw new IOException("Can't find a maven repository without both group & artifact id");
		}
		String groupId = id.substring(0, index);
		String artifactId = id.substring(index + 1);
		DomainRepository repository = ((EAIResourceRepository) entry.getRepository()).getMavenRepository();
		List<String> versions = new ArrayList<String>(repository.getVersions(groupId, artifactId));
		if (versions.isEmpty()) {
			throw new IOException("Can not find the artifact " + id);
		}
		be.nabu.libs.maven.api.Artifact mavenArtifact = repository.getArtifact(groupId, artifactId, versions.get(versions.size() - 1), false);
		if (mavenArtifact == null) {
			throw new IOException("Can not find the artifact " + id);
		}
		try {
			return new MavenArtifact(definedTypeResolver, new DependencyResolver(new URI("http://ibiblio.org/maven2")), repository, id, mavenArtifact);
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, MavenArtifact artifact) throws IOException {
		throw new IOException("Can not update a maven repository this way, please use the maven endpoint");
	}

	@Override
	public Class<MavenArtifact> getArtifactClass() {
		return MavenArtifact.class;
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry root, MavenArtifact artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		// if you are adding it to the actual repository root, first create an entry for the groupId
		// this allows you to define maven repositories in other places than the groupId
		if (root.getParent() == null) {
			root = getParent(root, artifact.getArtifact().getGroupId(), true);
		}
		List<String> keys = new ArrayList<String>(artifact.getChildren().keySet());
		Collections.sort(keys);
		for (String childId : keys) {
			ModifiableEntry parent = getParent(root, childId, false);
			int index = childId.lastIndexOf('.');
			String childName = prettify(index < 0 ? childId : childId.substring(index + 1));
			EAINode node = new EAINode();
			node.setArtifact(artifact.getChildren().get(childId));
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
			}
			node.setLeaf(true);
			MemoryEntry child = new MemoryEntry(root.getRepository(), parent, node, parent.getId() + "." + childName, childName);
			node.setEntry(child);
//			node.setEntry(parent);
			parent.addChildren(child);
		}
		return entries;
	}
	
	private ModifiableEntry getParent(ModifiableEntry root, String id, boolean includeLast) {
		ParsedPath path = new ParsedPath(id.replace('.', '/'));
		// resolve a parent path
		while ((includeLast && path != null) || (!includeLast && path.getChildPath() != null)) {
			Entry entry = root.getChild(prettify(path.getName()));
			// if it's null, create a new entry
			if (entry == null) {
				entry = new MemoryEntry(root.getRepository(), root, null, (root.getId().isEmpty() ? "" : root.getId() + ".") + prettify(path.getName()), prettify(path.getName()));
				root.addChildren(entry);
			}
			else if (entry.isNode()) {
				((EAINode) entry.getNode()).setLeaf(false);
			}
			root = (ModifiableEntry) entry;
			path = path.getChildPath();
		}
		return root;
	}
	
	private String prettify(String name) {
		return name.substring(0, 1).toLowerCase() + name.substring(1);
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry root, MavenArtifact artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		if (root.getParent() == null) {
			root = getParent(root, artifact.getArtifact().getGroupId(), true);
		}
		for (String id : artifact.getChildren().keySet()) {
			int index = id.lastIndexOf('.');
			ModifiableEntry parent = index < 0 ? root : getParent(root, id, false);
			String name = prettify(index < 0 ? id : id.substring(index + 1));
			parent.removeChildren(name);
			// if no children are remaining, remove parent as well
			if (!parent.iterator().hasNext()) {
				if (parent.getParent() instanceof ModifiableEntry) {
					entries.add(parent);
					((ModifiableEntry) parent.getParent()).removeChildren(parent.getName());
				}
			}
		}
		return entries;
	}	
}

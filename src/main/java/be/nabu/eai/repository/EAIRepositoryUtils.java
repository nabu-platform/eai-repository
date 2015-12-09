package be.nabu.eai.repository;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ExtensibleEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.ParsedPath;

public class EAIRepositoryUtils {

	private static Logger logger = LoggerFactory.getLogger(EAIRepositoryUtils.class);
	
	public static Entry getEntry(Entry entry, String id) {
		ParsedPath path = new ParsedPath(id.replace('.', '/'));
		while (entry != null && path != null) {
			entry = entry.getChild(path.getName());
			path = path.getChildPath();
		}
		return entry;
	}
	
	public static Node getNode(Repository repository, String id) {
		Entry entry = getEntry(repository.getRoot(), id);
		return entry != null && entry.isNode() ? entry.getNode() : null;
	}
	
	public static Artifact resolve(Repository repository, String id) {
		Entry entry = getEntry(repository.getRoot(), id);
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
	
	@SuppressWarnings("unchecked")
	public static <T extends Artifact> List<T> getArtifacts(Repository repository, Class<T> artifactClazz) {
		List<T> artifacts = new ArrayList<T>();
		for (Node node : repository.getNodes(artifactClazz)) {
			try {
				artifacts.add((T) node.getArtifact());
			}
			catch (Exception e) {
				logger.error("Could not load node: " + node);
			}
		}
		return artifacts;
	}

	public static Entry getDirectoryEntry(ResourceRepository repository, String id, boolean create) throws IOException {
		ParsedPath path = new ParsedPath(id.replace(".", "/"));
		Entry entry = repository.getRoot();
		while (path != null) {
			Entry child = entry.getChild(path.getName());
			if (child == null && create && entry instanceof ExtensibleEntry) {
				child = ((ExtensibleEntry) entry).createDirectory(path.getName());
			}
			if (child == null) {
				return null;
			}
			entry = child;
			path = path.getChildPath();
		}
		return entry;
	}
}

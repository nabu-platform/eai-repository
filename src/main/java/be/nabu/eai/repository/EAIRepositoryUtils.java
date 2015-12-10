package be.nabu.eai.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ExtensibleEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.FiniteResource;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.types.ParsedPath;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

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
	
	public static void zip(OutputStream output, Entry entry, EntryFilter acceptor) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(output);
		try {
			zipEntry(zip, entry, acceptor);
		}
		finally {
			zip.finish();
		}
	}
	
	private static void zipEntry(ZipOutputStream output, Entry entry, EntryFilter acceptor) throws IOException {
		if (entry instanceof ResourceEntry && entry.isNode() && (acceptor == null || acceptor.accept((ResourceEntry) entry))) {
			zipNode(output, entry.getId().replace(".", "/"), ((ResourceEntry) entry).getContainer(), ((ResourceEntry) entry).getRepository());
		}
		if (!entry.isLeaf() && (acceptor == null || acceptor.recurse((ResourceEntry) entry))) {
			for (Entry child : entry) {
				if (child instanceof ResourceEntry) {
					zipEntry(output, (ResourceEntry) child, acceptor);
				}
			}
		}
	}
	
	private static void zipNode(ZipOutputStream output, String path, ResourceContainer<?> container, ResourceRepository repository) throws IOException {
		for (Resource resource : container) {
			String childPath = path + "/" + resource.getName();
			if (resource instanceof ReadableResource) {
				ZipEntry entry = new ZipEntry(childPath);
				if (resource instanceof FiniteResource) {
					entry.setSize(((FiniteResource) resource).getSize());
				}
				if (resource instanceof TimestampedResource) {
					entry.setLastModifiedTime(FileTime.fromMillis(((TimestampedResource) resource).getLastModified().getTime()));
				}
				output.putNextEntry(entry);
				ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
				try {
					IOUtils.copyBytes(readable, IOUtils.wrap(output));
				}
				finally {
					readable.close();
				}
			}
			else if (resource instanceof ResourceContainer && repository.isInternal((ResourceContainer<?>) resource)) {
				zipNode(output, childPath, (ResourceContainer<?>) resource, repository);
			}
		}
	}
	
	public static interface EntryFilter {
		public boolean accept(ResourceEntry entry);
		public boolean recurse(ResourceEntry entry);
	}
}

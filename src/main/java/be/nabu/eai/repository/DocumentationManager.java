package be.nabu.eai.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.nabu.eai.repository.api.Documented;
import be.nabu.eai.repository.api.Documentor;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class DocumentationManager {
	
	// this concerns manually written documentation
	public static Documented getDocumentation(Repository repository, String id) {
		Entry entry = repository.getEntry(id);
		if (entry instanceof ResourceEntry) {
			ResourceContainer<?> protectedFolder = (ResourceContainer<?>) ((ResourceEntry) entry).getContainer().getChild(EAIResourceRepository.PROTECTED);
			if (protectedFolder != null) {
				ResourceContainer<?> documentationFolder = (ResourceContainer<?>) protectedFolder.getChild("documentation");
				if (documentationFolder != null) {
					Resource child = documentationFolder.getChild("readme.md");
					DocumentedImpl documented = null;
					if (child != null) {
						documented = read(child);
					}
					child = documentationFolder.getChild("readme.html");
					if (child != null) {
						documented = readHtml(child);
					}
					for (Resource documentChild : documentationFolder) {
						// if it is a readable file, add it
						if (documentChild instanceof ReadableResource && !documentChild.getName().equals("readme.md") && !documentChild.getName().equals("readme.html")) {
							if (documentChild.getName().endsWith(".md")) {
								documented.getFragments().add(read(documentChild));
							}
							else if (documentChild.getName().endsWith(".html")) {
								documented.getFragments().add(readHtml(documentChild));
							}
						}
					}
					return documented;
				}
			}
		}
		return null;
	}

	// this concerns automatically generated documentation
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Documented generateDocumentation(Repository repository, String id) {
		Artifact artifact = repository.resolve(id);
		if (artifact == null) {
			return null;
		}
		Documentor documentor = getDocumentor(artifact);
		return documentor == null ? null : documentor.getDocumentation(repository, artifact);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Documentor getDocumentor(Artifact artifact) {
		Documentor closest = null;
		for (Documentor<?> documentor : ServiceLoader.load(Documentor.class)) {
			if (documentor.getDocumentedClass().isAssignableFrom(artifact.getClass())) {
				if (closest == null || closest.getDocumentedClass().isAssignableFrom(documentor.getDocumentedClass())) {
					closest = documentor;
				}
			}
		}
		return closest;
	}
	
	public static boolean canGenerate(Repository repository, String id) {
		Artifact artifact = repository.resolve(id);
		if (artifact == null) {
			return false;
		}
		return getDocumentor(artifact) != null;
	}
	
	private static DocumentedImpl readHtml(Resource child) {
		try {
			ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable();
			try {
				byte[] bytes = IOUtils.toBytes(readable);
				String content = new String(bytes, "UTF-8");
				Collection<String> tags = null;
				String title = null;
				Pattern pattern = Pattern.compile("<meta[\\s]+name=\"([^\"]+)\"[\\s]+content=\"([^\"]+)\"[^>]*>");
				Matcher matcher = pattern.matcher(content);
				while (matcher.find()) {
					String name = matcher.group(1);
					if ("tags".equals(name)) {
						tags = Arrays.asList(matcher.group(2).split("[\\s]*,[\\s]*"));
					}
					else if ("title".equals(name)) {
						title = matcher.group(2);
					}
				}
				DocumentedImpl documented = new DocumentedImpl("text/html");
				documented.setTags(tags);
				documented.setTitle(title);
				documented.setDescription(content.replaceAll("<meta name=\"(tags|title)\"[^>]+>", ""));
				return documented;
			}
			finally {
				readable.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static DocumentedImpl read(Resource child) {
		if ("text/html".equals(child.getContentType())) {
			return readHtml(child);
		}
		try {
			ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable();
			try {
				byte[] bytes = IOUtils.toBytes(readable);
				String content = new String(bytes, "UTF-8");
				String annotationBlock = content.replaceAll("(?s)^([\\s]*@.*?)[\n]+(?!@).*", "$1");
				Collection<String> tags = null;
				String title = null;
				// no annotations found
				if (annotationBlock.trim().startsWith("@")) {
					// strip the block
					content = content.substring(annotationBlock.length());
					for (String line : annotationBlock.split("\n")) {
						if (line.trim().startsWith("@")) {
							line = line.substring(1);
							String [] parts = line.split("(=| )", 2);
							if ("tags".equals(parts[0].trim()) && parts.length > 1 && !parts[1].trim().isEmpty()) {
								tags = new ArrayList<String>(Arrays.asList(parts[1].trim().split("[\\s]*,[\\s]*")));
							}
							else if ("title".equals(parts[0].trim()) && parts.length > 1 && !parts[1].trim().isEmpty()) {
								title = parts[1].trim();
							}
						}
					}
				}
				DocumentedImpl documented = new DocumentedImpl();
				documented.setTags(tags);
				documented.setTitle(title);
				// remove leading whitespace
				documented.setDescription(content.replaceFirst("^[\\s]*", ""));
				return documented;
			}
			finally {
				readable.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void writeHtml(Resource child, Documented documented) {
		String content = "";
		if (documented.getTitle() != null) {
			content += "<meta name=\"title\" content=\"" + documented.getTitle() + "\"/>";
		}
		if (documented.getTags() != null) {
			StringBuilder builder = new StringBuilder();
			for (String tag : documented.getTags()) {
				if (!builder.toString().isEmpty()) {
					builder.append(", ");
				}
				builder.append(tag);
			}
			if (!builder.toString().isEmpty()) {
				content += "<meta name=\"tags\" content=\"" + builder.toString() + "\"/>";
			}
		}
		content += documented.getDescription();
		try {
			WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
			try {
				writable.write(IOUtils.wrap(content.getBytes("UTF-8"), true));
			}
			finally {
				writable.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void write(Resource child, Documented documented) {
		if ("text/html".equals(documented.getMimeType())) {
			writeHtml(child, documented);
		}
		else {
			String content = "";
			if (documented.getTitle() != null) {
				content += "@title " + documented.getTitle() + "\n";
			}
			if (documented.getTags() != null) {
				StringBuilder builder = new StringBuilder();
				for (String tag : documented.getTags()) {
					if (!builder.toString().isEmpty()) {
						builder.append(", ");
					}
					builder.append(tag);
				}
				if (!builder.toString().isEmpty()) {
					content += "@tags " + builder.toString() + "\n";
				}
			}
			if (!content.isEmpty()) {
				content += "\n";
			}
			content += documented.getDescription().replaceFirst("^[\\s]*", "");
			try {
				WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
				try {
					writable.write(IOUtils.wrap(content.getBytes("UTF-8"), true));
				}
				finally {
					writable.close();
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static class DocumentedImpl implements Documented {
		private String title, description;
		private Collection<String> tags;
		private String mimeType;
		private List<Documented> fragments = new ArrayList<Documented>();
		
		public DocumentedImpl() {
			this("text/x-markdown");
		}
		public DocumentedImpl(String mimeType) {
			this.mimeType = mimeType;
		}
		
		@Override
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		
		@Override
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		
		@Override
		public Collection<String> getTags() {
			return tags;
		}
		public void setTags(Collection<String> tags) {
			this.tags = tags;
		}
		@Override
		public String getMimeType() {
			return mimeType;
		}
		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}
		@Override
		public List<Documented> getFragments() {
			return fragments;
		}
		public void setFragments(List<Documented> fragments) {
			this.fragments = fragments;
		}
	}
}

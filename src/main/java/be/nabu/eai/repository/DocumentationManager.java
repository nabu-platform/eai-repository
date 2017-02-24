package be.nabu.eai.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import be.nabu.eai.repository.api.Documented;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class DocumentationManager {
	
	public static Documented getDocumentation(Repository repository, String id) {
		Entry entry = repository.getEntry(id);
		if (entry instanceof ResourceEntry) {
			ResourceContainer<?> protectedFolder = (ResourceContainer<?>) ((ResourceEntry) entry).getContainer().getChild(EAIResourceRepository.PROTECTED);
			if (protectedFolder != null) {
				ResourceContainer<?> documentationFolder = (ResourceContainer<?>) protectedFolder.getChild("documentation");
				if (documentationFolder != null) {
					Resource child = documentationFolder.getChild("readme.md");
					if (child != null) {
						return read(child);
					}
				}
			}
		}
		return null;
	}

	public static DocumentedImpl read(Resource child) {
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
	
	public static void write(Resource child, Documented documented) {
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
		try {;
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
	
	public static class DocumentedImpl implements Documented {
		private String title, description;
		private Collection<String> tags;
		
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
	}
}

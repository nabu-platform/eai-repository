package be.nabu.eai.repository;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.LargeText;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.util.ClassAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.Validation;

@XmlRootElement(name = "node")
@SuppressWarnings("rawtypes")
public class EAINode implements Node {

	private Class<? extends ArtifactManager> artifactManager;
	private Class<? extends Artifact> artifactClass;
	private Artifact artifact;
	private List<String> references;
	private Entry entry;
	private List<Validation<?>> messages = new ArrayList<Validation<?>>();
	private Map<String, String> properties = new LinkedHashMap<String, String>();
	private long version;
	private Date lastModified, created, deprecated;
	private String environmentId;
	private boolean hidden;
	private String name, description, comment, summary;
	private List<String> tags;
	private String mergeScript;
	
	/**
	 * By default all nodes are leafs
	 * However, by indication of the user it may become a directory
	 * Note that this changes nothing for the node and is actually only intended to use in a tree display
	 */
	private boolean leaf = true;
	
	public void persist() {
		// TODO
	}
	
	@Override
	@XmlTransient
	public Artifact getArtifact() throws IOException, ParseException {
		if (artifact == null) {
			synchronized(this) {
				if (artifact == null) {
					messages.clear();
					artifact = newArtifactManager().load((ResourceEntry) entry, messages);
				}
			}
		}
		return artifact;
	}
	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}
	
	@Override
	@XmlAttribute
	public boolean isLeaf() {
		return leaf;
	}
	public void setLeaf(boolean leaf) {
		this.leaf = leaf;
	}

	@Override
	@XmlAttribute
	@XmlJavaTypeAdapter(ClassAdapter.class)
	public Class<? extends ArtifactManager> getArtifactManager() {
		return artifactManager;
	}
	public void setArtifactManager(Class<? extends ArtifactManager> artifactManager) {
		this.artifactManager = artifactManager;
	}

	@Override
	public List<String> getReferences() {
		if (references == null) {
			references = new ArrayList<String>();
		}
		return references;
	}
	public void setReferences(List<String> references) {
		this.references = references;
	}
	
	@Override
	@XmlTransient
	public Class<? extends Artifact> getArtifactClass() {
		if (artifactClass == null) {
			artifactClass = artifact != null ? artifact.getClass() : newArtifactManager().getArtifactClass();
		}
		return artifactClass;
	}
	
	public ArtifactManager<?> newArtifactManager() {
		if (getArtifactManager() == null) {
			throw new NullPointerException("No artifact manager configured for: " + getEntry().getId());
		}
		try {
			return getArtifactManager().newInstance();
		}
		catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@XmlTransient
	public Entry getEntry() {
		return entry;
	}
	public void setEntry(Entry entry) {
		this.entry = entry;
	}
	public void setArtifactClass(Class<? extends Artifact> artifactClass) {
		this.artifactClass = artifactClass;
	}

	@XmlJavaTypeAdapter(KeyValueMapAdapter.class)
	public Map<String, String> getProperties() {
		return properties;
	}
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	@XmlAttribute
	@Override
	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	@XmlAttribute
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	@XmlAttribute
	@Override
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	@Override
	public String getEnvironmentId() {
		return environmentId;
	}
	public void setEnvironmentId(String environmentId) {
		this.environmentId = environmentId;
	}

	@Override
	public boolean isLoaded() {
		return artifact != null;
	}
	
	@Override
	public String toString() {
		return "Node for: " + (entry == null ? "anonymous" : entry.getId());
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@XmlAttribute
	public Date getDeprecated() {
		return deprecated;
	}
	public void setDeprecated(Date deprecated) {
		this.deprecated = deprecated;
	}

	@Override
	@XmlAttribute
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@LargeText
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@LargeText
	@Override
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}

	@LargeText
	@Override
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	@Override
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	@LargeText
	@Override
	public String getMergeScript() {
		return mergeScript;
	}
	public void setMergeScript(String mergeScript) {
		this.mergeScript = mergeScript;
	}
	
}

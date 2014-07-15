package be.nabu.eai.repository;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;

@XmlRootElement(name = "node")
@SuppressWarnings("rawtypes")
public class EAINode implements Node {
	
	private Class<? extends ArtifactManager> artifactManager;
	private Artifact artifact;
	private List<String> references;
	private RepositoryEntry entry;
	
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
			artifact = newArtifactManager().load(entry);
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
	public Class<? extends ArtifactManager> getArtifactManager() {
		return artifactManager;
	}
	public void setArtifactManager(Class<? extends ArtifactManager> artifactManager) {
		this.artifactManager = artifactManager;
	}

	@Override
	public List<String> getReferences() {
		return references;
	}
	public void setReferences(List<String> references) {
		this.references = references;
	}
	
	@Override
	@XmlTransient
	public Class<? extends Artifact> getArtifactClass() {
		return newArtifactManager().getArtifactClass();
	}
	
	private ArtifactManager<?> newArtifactManager() {
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
	public RepositoryEntry getEntry() {
		return entry;
	}
	public void setEntry(RepositoryEntry entry) {
		this.entry = entry;
	}	
}

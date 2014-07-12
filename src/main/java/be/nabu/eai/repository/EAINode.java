package be.nabu.eai.repository;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.libs.artifacts.api.Artifact;

@XmlRootElement(name = "node")
public class EAINode {
	
	private Class<? extends ArtifactManager<?>> artifactManager;
	private EAIRepository repository;
	private Artifact artifact;
	private String id;
	private List<String> references;
	
	/**
	 * By default all nodes are leafs
	 * However, by indication of the user it may become a directory
	 * Note that this changes nothing for the node and is actually only intended to use in a tree display
	 */
	private boolean leaf = true;
	
	public void persist() {
		// TODO
	}
	
	@XmlTransient
	public Artifact getArtifact() {
		return artifact;
	}
	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}
	
	@XmlTransient
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@XmlTransient
	public EAIRepository getRepository() {
		return repository;
	}
	public void setRepository(EAIRepository repository) {
		this.repository = repository;
	}

	@XmlAttribute
	public boolean isLeaf() {
		return leaf;
	}
	public void setLeaf(boolean leaf) {
		this.leaf = leaf;
	}

	@XmlAttribute
	public Class<? extends ArtifactManager<?>> getArtifactManager() {
		return artifactManager;
	}
	public void setArtifactManager(Class<? extends ArtifactManager<?>> artifactManager) {
		this.artifactManager = artifactManager;
	}

	public List<String> getReferences() {
		return references;
	}
	public void setReferences(List<String> references) {
		this.references = references;
	}
}

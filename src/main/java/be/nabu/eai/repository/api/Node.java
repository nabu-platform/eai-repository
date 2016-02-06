package be.nabu.eai.repository.api;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;

public interface Node {
	/**
	 * The version indicates numerically which version a node is at
	 * It is not guaranteed to be unique across server but within a server it should be
	 */
	public long getVersion();
	/**
	 * The last modified date tells you when the node was last updated
	 */
	public Date getLastModified();
	/**
	 * The environment id tells you which environment it was last modified in
	 */
	public String getEnvironmentId();
	/**
	 * The references for this node, this is basically a cached list allowing you to build a reference/dependency map without actually loading the artifacts
	 * This is an optimization to allow for lazy loading but with full contextual awareness
	 */
	public List<String> getReferences();
	/**
	 * The artifact contained by this node, if not yet loaded, this will trigger a load
	 */
	public Artifact getArtifact() throws IOException, ParseException;
	/**
	 * The type of the artifact class (does not trigger a load)
	 */
	public Class<? extends Artifact> getArtifactClass();
	/**
	 * Whether or not this node is a leaf, a node can be both an artifact and a container of more artifacts
	 */
	public boolean isLeaf();
	/**
	 * Whether or not the artifact is actually loaded
	 */
	public boolean isLoaded();
	/**
	 * The artifact manager for this type of artifact
	 */
	@SuppressWarnings("rawtypes")
	public Class<? extends ArtifactManager> getArtifactManager();
	/**
	 * Any properties attached to this node
	 */
	public Map<String, String> getProperties();
}

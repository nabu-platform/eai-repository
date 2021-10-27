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
	 * The created date tells us when the node was first created
	 */
	public Date getCreated();
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
	
	/**
	 * You can hide nodes from the developer view
	 * The primary reason for this is "internal" services, so services you might want to use for internal stuff but not expose for developers to use
	 */
	public default boolean isHidden() { return false; }
	
	/**
	 * You can deprecate nodes, they stay active but are colored differently or hidden alltogether if you want
	 * The idea is that you want to warn people that something is deprecated meaning it might disappear in a while
	 * Perhaps we should have a multi-level approach: deprecate so it is visually different for a while
	 * Then also set it to hide so it is visually gone but things still work
	 * Then, after a long time, delete it.
	 */
	public default Date getDeprecated() { return null; }
	/**
	 * We can set a human readable name for the node
	 */
	public default String getName() { return null; }
	/**
	 * We can set a description, this is meant to be read by business people, not necessarily those building the application
	 */
	public default String getDescription() { return null; }
	/**
	 * A comment is meant to be read by people building the application, using developer
	 */
	public default String getComment() { return null; }
	/**
	 * A summary is meant to be read by outsiders
	 */
	public default String getSummary() { return null; }
	/**
	 * You can tag a node so we can group them together based on context
	 */
	public default List<String> getTags() { return null; }
	/**
	 * This should contain either a resolvable URI to the merge script, or the actual script itself
	 */
	public default String getMergeScript() { return null; }
}

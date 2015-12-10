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
	
	public List<String> getReferences();
	public Artifact getArtifact() throws IOException, ParseException;
	public Class<? extends Artifact> getArtifactClass();
	public boolean isLeaf();
	
	@SuppressWarnings("rawtypes")
	public Class<? extends ArtifactManager> getArtifactManager();
	public Map<String, String> getProperties();
}

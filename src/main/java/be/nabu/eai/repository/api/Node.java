package be.nabu.eai.repository.api;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;

public interface Node {
	public long getVersion();
	public Date getLastModified();
	
	public List<String> getReferences();
	public Artifact getArtifact() throws IOException, ParseException;
	public Class<? extends Artifact> getArtifactClass();
	public boolean isLeaf();
	
	@SuppressWarnings("rawtypes")
	public Class<? extends ArtifactManager> getArtifactManager();
	public Map<String, String> getProperties();
}

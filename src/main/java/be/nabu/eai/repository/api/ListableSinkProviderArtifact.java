package be.nabu.eai.repository.api;

import java.util.Date;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.metrics.core.api.ListableSinkProvider;

public interface ListableSinkProviderArtifact extends ListableSinkProvider, Artifact {
	public void setLastPushed(Date lastPushed);
	public Date getLastPushed();
	public Repository getRepository();
}

package be.nabu.eai.repository.api;

import java.nio.charset.Charset;
import java.util.List;

import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.services.api.DefinedServiceLister;
import be.nabu.libs.services.api.ExecutionContextProvider;
import be.nabu.libs.services.api.ServiceRunner;

public interface Repository extends ArtifactResolver<Artifact>, ExecutionContextProvider, DefinedServiceLister {
	public RepositoryEntry getRoot();
	public Entry getEntry(String id);
	public Charset getCharset();
	public EventDispatcher getEventDispatcher();
	public Node getNode(String id);
	public void reload(String id);
	public void unload(String id);
	public List<Node> getNodes(Class<? extends Artifact> artifactClazz);
	public ServiceRunner getServiceRunner();
	public void setServiceRunner(ServiceRunner serviceRunner);
	public List<String> getReferences(String id);
	public List<String> getDependencies(String id);
	public void start();
	public ClassLoader newClassLoader(String artifact);
}

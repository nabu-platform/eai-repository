package be.nabu.eai.repository.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.metrics.api.MetricProvider;
import be.nabu.libs.services.api.DefinedServiceLister;
import be.nabu.libs.services.api.ExecutionContextProvider;
import be.nabu.libs.services.api.ServiceRunner;

public interface Repository extends ArtifactResolver<Artifact>, ExecutionContextProvider, DefinedServiceLister, MetricProvider {
	/**
	 * Get an entry in this repository
	 */
	public Entry getEntry(String id);
	/**
	 * A shortcut to get the root entry for this repository
	 */
	public Entry getRoot();
	/**
	 * The charset that the repository uses
	 */
	public Charset getCharset();
	/**
	 * The event dispatcher used to send repository/node events
	 */
	public EventDispatcher getEventDispatcher();
	/**
	 * The event dispatcher used to send metrics events, this could be the same as the above
	 */
	public EventDispatcher getMetricsDispatcher();
	/**
	 * Returns a node in the repository, it wraps around an artifact and adds meta data to it, for example the version
	 */
	public Node getNode(String id);
	/**
	 * Reload a single id
	 */
	public void reload(String id);
	/**
	 * Reload everything
	 */
	public void reloadAll();
	/**
	 * Reload a collection of ids
	 */
	public void reloadAll(Collection<String> ids);
	/**
	 * Unload an id
	 */
	public void unload(String id);
	/**
	 * Get all the references for a given id
	 */
	public List<String> getReferences(String id);
	/**
	 * Get all the dependencies for a given id
	 */
	public List<String> getDependencies(String id);
	/**
	 * Get the service runner registered with this repository
	 */
	public ServiceRunner getServiceRunner();
	/**
	 * Register a new service runner for this repository
	 */
	public void setServiceRunner(ServiceRunner serviceRunner);
	/**
	 * Start the repository, this will load everything in it (possibly lazily)
	 */
	public void start();
	/**
	 * Returns a repository-aware classloader
	 */
	public ClassLoader getClassLoader();
	/**
	 * Get all the artifacts of a certain type
	 */
	public <T> List<T> getArtifacts(Class<T> ifaceClass);
	
	default String getName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			return "anonymous";
		}
	}
}

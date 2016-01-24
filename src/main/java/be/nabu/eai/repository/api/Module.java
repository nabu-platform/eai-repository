package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

/**
 * The id of the artifact is what will be used to define dependencies
 */
public interface Module extends Artifact {
	/**
	 * A logical name of the module
	 */
	public String getName();
	/**
	 * The version of the module
	 */
	public String getVersion();
	/**
	 * A description of the module
	 */
	public String getDescription();
	/**
	 * The modules this module depends on to function
	 */
	public List<Module> getDependencies();
}

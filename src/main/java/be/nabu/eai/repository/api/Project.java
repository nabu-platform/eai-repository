package be.nabu.eai.repository.api;

import java.util.List;

public interface Project {
	/**
	 * A stringified type of the project, can (amongst other things) be used to find an appropriate icon
	 * In the future we might have a "ProjectManager" and/or "ProjectGUIManager" class, but currently there is no need for it yet (except perhaps the icon resolving which is a minor requirement for such a heavy feature)
	 */
	public String getType();
	/**
	 * We can set a human readable name for the node
	 */
	public String getName();
	/**
	 * We can set a description, this is meant to be read by business people
	 */
	public String getDescription();
	/**
	 * A comment is meant to be read by technical people 
	 */
	public String getComment();
	/**
	 * A summary is meant to be read by outsiders
	 */
	public String getSummary();
	/**
	 * You can tag a node so we can group them together based on context
	 */
	public List<String> getTags();
	/**
	 * Versioning and deployment are done at the project level
	 * By having quick access to the version, we can do things like compare environments
	 */
	public String getVersion();
}

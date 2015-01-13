package be.nabu.eai.repository;

import java.security.Principal;

public class InternalPrincipal implements Principal {

	/**
	 * The name of the user
	 */
	private String name;
	
	/**
	 * The id of the source artifact that set the principal
	 * For example when a trigger starts a service, it needs a principal
	 * This is an internal one set with the id of the trigger
	 */
	private String sourceId;
	
	public InternalPrincipal(String name, String sourceId) {
		this.name = name;
		this.sourceId = sourceId;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public String getSourceId() {
		return sourceId;
	}
}

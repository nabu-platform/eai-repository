package be.nabu.eai.repository.util;

import java.security.Principal;

/**
 * A principal as instantiated by the system
 * This is _not_ validated against a backend using credentials
 */
public class SystemPrincipal implements Principal {

	public static final SystemPrincipal ROOT = new SystemPrincipal("root");
	
	private String name;

	public SystemPrincipal(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
}

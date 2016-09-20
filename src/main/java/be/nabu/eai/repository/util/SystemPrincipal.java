package be.nabu.eai.repository.util;

import java.security.Principal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import be.nabu.libs.authentication.api.Token;

/**
 * A principal as instantiated by the system
 * This is _not_ validated against a backend using credentials
 */
public class SystemPrincipal implements Token {

	private static final long serialVersionUID = 1L;

	public static final SystemPrincipal ROOT = new SystemPrincipal("root");
	
	private String name;

	private List<Principal> credentials;

	public SystemPrincipal() {
		// auto construct
	}
	
	public SystemPrincipal(String name, Principal...credentials) {
		this.name = name;
		this.credentials = Arrays.asList(credentials);
	}
	
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getRealm() {
		return "$system";
	}

	@Override
	public Date getValidUntil() {
		return null;
	}

	@Override
	public List<Principal> getCredentials() {
		return credentials;
	}
}

/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
	public static final SystemPrincipal GUEST = new SystemPrincipal("guest");
	
	private String name, realm;

	private List<Principal> credentials;

	public SystemPrincipal() {
		// auto construct
	}
	
	public SystemPrincipal(String name, Principal...credentials) {
		this(name, null, credentials);
	}
	
	public SystemPrincipal(String name, String realm, Principal...credentials) {
		this.name = name;
		this.realm = realm;
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
		return realm == null ? "$system" : realm;
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

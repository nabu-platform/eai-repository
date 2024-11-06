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

import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.authentication.api.principals.DevicePrincipal;
import be.nabu.libs.authentication.api.principals.SharedSecretPrincipal;

public class CombinedAuthenticator implements Authenticator {

	private SecretAuthenticator secretAuthenticator;
	private PasswordAuthenticator passwordAuthenticator;
	
	public CombinedAuthenticator(PasswordAuthenticator passwordAuthenticator, SecretAuthenticator secretAuthenticator) {
		this.passwordAuthenticator = passwordAuthenticator;
		this.secretAuthenticator = secretAuthenticator;
	}
	
	@Override
	public Token authenticate(String realm, Principal... credentials) {
		// make sure we get the device (if possible)
		Device device = null;
		for (Principal credential : credentials) {
			if (credential instanceof DevicePrincipal) {
				device = ((DevicePrincipal) credential).getDevice();
				if (device != null) {
					break;
				}
			}
		}
		for (Principal credential : credentials) {
			if (credential instanceof BasicPrincipal && passwordAuthenticator != null) {
				Token token = passwordAuthenticator.authenticate(realm, (BasicPrincipal) credential, device);
				if (token != null) {
					return token;
				}
			}
			else if (credential instanceof SharedSecretPrincipal && secretAuthenticator != null) {
				Token token = secretAuthenticator.authenticate(realm, (SharedSecretPrincipal) credential, device);
				if (token != null) {
					return token;
				}
			}
		}
		return null;
	}
	
}

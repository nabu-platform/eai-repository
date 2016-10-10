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
		for (Principal credential : credentials) {
			Device device = null;
			if (credential instanceof DevicePrincipal) {
				device = ((DevicePrincipal) credential).getDevice();
			}
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

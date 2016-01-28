package be.nabu.eai.repository.api;

import be.nabu.libs.authentication.api.Authenticator;

public interface AuthenticatorProvider {
	public String getRealm();
	public Authenticator getAuthenticator();
}

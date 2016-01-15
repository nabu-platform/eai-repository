package be.nabu.eai.repository.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.authentication.api.Token;

public interface UserLanguageProvider {
	@WebResult(name = "language")
	public String getLanguage(@WebParam(name = "token") Token token);
}

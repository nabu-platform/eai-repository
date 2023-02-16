package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.server.AuthenticationHeader;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;

public class AuthenticationEnricher implements EventEnricher {
	
	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		Token token = getTokenAnywhere();
		if (token != null && object != null) {
			if (!(object instanceof ComplexContent)) {
				object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
			}
			if (object != null) {
				// if we have a field called "sessionId", we enrich it
				if (token.getAuthenticationId() != null && ((ComplexContent) object).getType().get("authenticationId") != null) {
					Object current = ((ComplexContent) object).get("authenticationId");
					if (current == null) {
						((ComplexContent) object).set("authenticationId", token.getAuthenticationId());
					}
				}
				if (token.getName() != null && ((ComplexContent) object).getType().get("alias") != null) {
					Object current = ((ComplexContent) object).get("alias");
					if (current == null) {
						((ComplexContent) object).set("alias", token.getName());
					}
				}
				if (token.getRealm() != null && ((ComplexContent) object).getType().get("realm") != null) {
					Object current = ((ComplexContent) object).get("realm");
					if (current == null) {
						((ComplexContent) object).set("realm", token.getRealm());
					}
				}
				if (token.getImpersonator() != null && ((ComplexContent) object).getType().get("impersonator") != null) {
					Object current = ((ComplexContent) object).get("impersonator");
					if (current == null) {
						((ComplexContent) object).set("impersonator", token.getImpersonator());
					}
				}
				if (token.getImpersonatorId() != null && ((ComplexContent) object).getType().get("impersonatorId") != null) {
					Object current = ((ComplexContent) object).get("impersonatorId");
					if (current == null) {
						((ComplexContent) object).set("impersonatorId", token.getImpersonatorId());
					}
				}
				if (token.getImpersonatorRealm() != null && ((ComplexContent) object).getType().get("impersonatorRealm") != null) {
					Object current = ((ComplexContent) object).get("impersonatorRealm");
					if (current == null) {
						((ComplexContent) object).set("impersonatorRealm", token.getImpersonatorRealm());
					}
				}
			}
		}
		return null;
	}
	
	private static Token getTokenAnywhere() {
		Token token = null;
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime != null) {
			token = runtime.getExecutionContext().getSecurityContext().getToken();
		}
		if (token == null) {
			token = getTokenFromRequest();
		}
		return token;
	}
	
	private static Token getTokenFromRequest() {
		Object currentRequest = RequestProcessor.getCurrentRequest();
		if (currentRequest instanceof HTTPRequest) {
			AuthenticationHeader authenticationHeader = HTTPUtils.getAuthenticationHeader((HTTPRequest) currentRequest);
			return authenticationHeader != null ? authenticationHeader.getToken() : null;
		}
		return null;
	}
}

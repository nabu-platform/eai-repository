package be.nabu.eai.repository.artifacts.web.rest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GlueListener.PathAnalysis;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeUtils;

public class WebRestListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private PathAnalysis pathAnalysis;
	private WebRestArtifactConfiguration configuration;
	private List<DefinedService> services;
	private DefinedServiceInterface iface;

	public WebRestListener(WebRestArtifactConfiguration configuration, DefinedServiceInterface iface, List<DefinedService> services) {
		this.configuration = configuration;
		this.pathAnalysis = GlueListener.analyzePath(configuration.getPath());
		this.iface = iface;
		this.services = services;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		try {
			URI uri = HTTPUtils.getURI(request, false);
			String path = URIUtils.normalize(uri.getPath());
			// not in this web artifact
			if (!path.startsWith(configuration.getWebArtifact().getConfiguration().getPath())) {
				return null;
			}
			path = path.substring(configuration.getWebArtifact().getConfiguration().getPath().length());
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			Map<String, String> analyzed = pathAnalysis.analyze(path);
			// not in this rest path
			if (analyzed == null) {
				return null;
			}
			Map<String, List<String>> cookies = HTTPUtils.getCookies(request.getContent().getHeaders());
			String originalSessionId = GlueListener.getSessionId(cookies);
			GlueListener listener = configuration.getWebArtifact().getListener();
			Session session = originalSessionId == null ? null : listener.getSessionProvider().getSession(originalSessionId);
			// check validity of token
			if (listener.getTokenValidator() != null) {
				Token token = session == null ? null : (Token) session.get(GlueListener.buildTokenName(listener.getRealm()));
				if (token != null && !listener.getTokenValidator().isValid(token)) {
					session.destroy();
					originalSessionId = null;
					session = null;
				}
			}
			// check permissions
			if (listener.getPermissionHandler() != null) {
				Token token = session == null ? null : (Token) session.get(GlueListener.buildTokenName(listener.getRealm()));
				if (!listener.getPermissionHandler().hasPermission(token, path, request.getMethod().toLowerCase())) {
					throw new HTTPException(401, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have permission to '" + request.getMethod().toLowerCase() + "' on: " + path);
				}
			}
			// check role
			if (listener.getRoleHandler() != null && configuration.getRole() != null) {
				Token token = session == null ? null : (Token) session.get(GlueListener.buildTokenName(listener.getRealm()));
				if (!listener.getRoleHandler().hasRole(token, configuration.getRole())) {
					throw new HTTPException(401, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have the required role: " + configuration.getRole());
				}
			}
			
			Map<String, List<String>> queryProperties = URIUtils.getQueryProperties(uri);
			
			ComplexContent input = iface.getInputDefinition().newInstance();
			for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("query").getType())) {
				input.set("query/" + element.getName(), queryProperties.get(element.getName()));
			}
			for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("header").getType())) {
				int counter = 0;
				for (Header header : MimeUtils.getHeaders(element.getName())) {
					input.set("header/" + element.getName() + "[" + counter++ + "]", header.getValue());
				}
			}
			if (session != null) {
				for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("session").getType())) {
					input.set("session/" + element.getName(), session.get(element.getName()));
				}
			}
			for (String key : analyzed.keySet()) {
				input.set("path/" + key, analyzed.get(key));
			}
			for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("cookie").getType())) {
				input.set("cookie/" + element.getName(), cookies.get(element.getName()));
			}
			
			// TODO: actually call the service and map back the content!
		}
		catch (IOException e) {
			throw new HTTPException(500, e);
		}
		catch (FormatException e) {
			throw new HTTPException(500, e);
		}
		return null;
	}

}

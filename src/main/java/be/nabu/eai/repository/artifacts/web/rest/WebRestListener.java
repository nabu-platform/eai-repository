package be.nabu.eai.repository.artifacts.web.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.AuthenticationHeader;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GlueListener.PathAnalysis;
import be.nabu.libs.http.glue.impl.ResponseMethods;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.api.UnmarshallableBinding;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class WebRestListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private PathAnalysis pathAnalysis;
	private String serverPath;
	private TokenValidator tokenValidator;
	private RoleHandler roleHandler;
	private PermissionHandler permissionHandler;
	private SessionProvider sessionProvider;
	private String realm;
	private DefinedService service;
	private Charset charset;
	private boolean allowEncoding;
	private Repository repository;
	private WebRestArtifact webArtifact;

	public WebRestListener(Repository repository, String serverPath, String realm, SessionProvider sessionProvider, PermissionHandler permissionHandler, RoleHandler roleHandler, TokenValidator tokenValidator, WebRestArtifact webArtifact, DefinedService service, Charset charset, boolean allowEncoding) throws IOException {
		this.repository = repository;
		this.serverPath = serverPath;
		this.realm = realm;
		this.sessionProvider = sessionProvider;
		this.permissionHandler = permissionHandler;
		this.roleHandler = roleHandler;
		this.tokenValidator = tokenValidator;
		this.webArtifact = webArtifact;
		this.service = service;
		this.charset = charset;
		this.allowEncoding = allowEncoding;
		String path = webArtifact.getConfiguration().getPath();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		this.pathAnalysis = GlueListener.analyzePath(path);
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		try {
			// stop fast if wrong method
			if (!webArtifact.getConfiguration().getMethod().toString().equalsIgnoreCase(request.getMethod())) {
				return null;
			}
			URI uri = HTTPUtils.getURI(request, false);
			String path = URIUtils.normalize(uri.getPath());
			// not in this web artifact
			if (!path.startsWith(serverPath)) {
				return null;
			}
			path = path.substring(serverPath.length());
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			Map<String, String> analyzed = pathAnalysis.analyze(path);
			System.out.println("Analyzed: " + analyzed + " in path: " + webArtifact.getConfiguration().getPath());
			// not in this rest path
			if (analyzed == null) {
				return null;
			}
			Map<String, List<String>> cookies = HTTPUtils.getCookies(request.getContent().getHeaders());
			String originalSessionId = GlueListener.getSessionId(cookies);
			Session session = originalSessionId == null ? null : sessionProvider.getSession(originalSessionId);
			
			// authentication tokens in the request get precedence over session-based authentication
			AuthenticationHeader authenticationHeader = HTTPUtils.getAuthenticationHeader(request);
			Token token = authenticationHeader == null ? null : authenticationHeader.getToken();
			// but likely we'll have to check the session for tokens
			if (token == null && session != null) {
				token = (Token) session.get(GlueListener.buildTokenName(realm));
			}
			else if (token != null && session != null) {
				session.set(GlueListener.buildTokenName(realm), token);
			}
			
			ExecutionContext.CONTEXT.set(repository.newExecutionContext(token));
			// check validity of token
			if (tokenValidator != null) {
				if (token != null && !tokenValidator.isValid(token)) {
					session.destroy();
					originalSessionId = null;
					session = null;
					token = null;
					ExecutionContext.CONTEXT.set(repository.newExecutionContext(null));
				}
			}
			// check permissions
			if (permissionHandler != null) {
				if (!permissionHandler.hasPermission(token, path, request.getMethod().toLowerCase())) {
					throw new HTTPException(401, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have permission to '" + request.getMethod().toLowerCase() + "' on: " + path);
				}
			}
			// check role
			if (roleHandler != null && webArtifact.getConfiguration().getRole() != null) {
				if (!roleHandler.hasRole(token, webArtifact.getConfiguration().getRole())) {
					throw new HTTPException(401, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have the required role: " + webArtifact.getConfiguration().getRole());
				}
			}
			
			Map<String, List<String>> queryProperties = URIUtils.getQueryProperties(uri);
			
			ComplexContent input = webArtifact.getInputDefinition().newInstance();
			if (input.getType().get("query") != null) {
				for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("query").getType())) {
					input.set("query/" + element.getName(), queryProperties.get(element.getName()));
				}
			}
			if (input.getType().get("header") != null) {
				for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("header").getType())) {
					int counter = 0;
					for (Header header : MimeUtils.getHeaders(element.getName())) {
						input.set("header/" + element.getName() + "[" + counter++ + "]", header.getValue());
					}
				}
			}
			if (session != null && input.getType().get("session") != null) {
				for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("session").getType())) {
					input.set("session/" + element.getName(), session.get(element.getName()));
				}
			}
			for (String key : analyzed.keySet()) {
				input.set("path/" + key, analyzed.get(key));
			}
			if (input.getType().get("cookie") != null) {
				for (Element<?> element : TypeUtils.getAllChildren((ComplexType) input.getType().get("cookie").getType())) {
					input.set("cookie/" + element.getName(), cookies.get(element.getName()));
				}
			}
			if (input.getType().get("content") != null && request.getContent() instanceof ContentPart) {
				String contentType = MimeUtils.getContentType(request.getContent().getHeaders());
				UnmarshallableBinding binding;
				if (contentType == null) {
					throw new HTTPException(400, "Unknown request content type");
				}
				else if (contentType.equalsIgnoreCase("application/xml") || contentType.equalsIgnoreCase("text/xml")) {
					binding = new XMLBinding((ComplexType) input.getType().get("content").getType(), charset);
				}
				else if (contentType.equalsIgnoreCase("application/json")) {
					binding = new JSONBinding((ComplexType) input.getType().get("content").getType(), charset);
				}
				else {
					throw new HTTPException(400, "Unsupported request content type: " + contentType);	
				}
				try {
					input.set("content", binding.unmarshal(IOUtils.toInputStream(((ContentPart) request.getContent()).getReadable()), new Window[0]));
				}
				catch (IOException e) {
					throw new HTTPException(500, e);
				}
				catch (ParseException e) {
					throw new HTTPException(400, "Message can not be parsed using specification: " + input.getType().get("content").getType(),e);
				}
			}
			
			Future<ServiceResult> run = repository.getServiceRunner().run(service, repository.newExecutionContext(token), input, null);
			if (webArtifact.getConfiguration().getAsynchronous() != null && webArtifact.getConfiguration().getAsynchronous()) {
				return HTTPUtils.newEmptyResponse();
			}
			else {
				ServiceResult serviceResult = run.get();
				if (serviceResult.getException() != null) {
					throw new HTTPException(500, serviceResult.getException());
				}
				ComplexContent output = serviceResult.getOutput();
				// if there is no content to respond with, just send back an empty response
				if (output == null || output.get("content") == null) {
					return HTTPUtils.newEmptyResponse();
				}
				else {
					output = (ComplexContent) output.get("content");
					List<String> acceptedContentTypes = request.getContent() != null
						? MimeUtils.getAcceptedContentTypes(request.getContent().getHeaders())
						: new ArrayList<String>();
					acceptedContentTypes.retainAll(ResponseMethods.allowedTypes);
					String contentType = acceptedContentTypes.isEmpty() ? webArtifact.getConfiguration().getPreferredResponseType().getMimeType() : acceptedContentTypes.get(0);
					MarshallableBinding binding;
					if (contentType.equalsIgnoreCase("application/xml")) {
						binding = new XMLBinding(output.getType(), charset);
					}
					else if (contentType.equalsIgnoreCase("application/json")) {
						binding = new JSONBinding(output.getType(), charset);
					}
					else {
						throw new HTTPException(500, "Unsupported response content type: " + contentType);
					}
					ByteArrayOutputStream content = new ByteArrayOutputStream();
					binding.marshal(content, (ComplexContent) output);
					byte[] byteArray = content.toByteArray();
					PlainMimeContentPart part = new PlainMimeContentPart(null,
						IOUtils.wrap(byteArray, true),
						new MimeHeader("Content-Length", "" + byteArray.length),
						new MimeHeader("Content-Type", contentType + "; charset=" + charset.name())
					);
					if (allowEncoding) {
						HTTPUtils.setContentEncoding(part, request.getContent().getHeaders());
					}
					return new DefaultHTTPResponse(200, HTTPCodes.getMessage(200), part);
				}
			}
		}
		catch (FormatException e) {
			throw new HTTPException(500, e);
		}
		catch (InterruptedException e) {
			throw new HTTPException(500, e);
		}
		catch (ExecutionException e) {
			throw new HTTPException(500, e);
		}
		catch (IOException e) {
			throw new HTTPException(500, e);
		}
		finally {
			ExecutionContext.CONTEXT.remove();
		}
	}

}

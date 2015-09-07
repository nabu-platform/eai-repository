package be.nabu.eai.repository.artifacts.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.artifacts.web.rest.WebRestArtifact;
import be.nabu.eai.repository.artifacts.web.rest.WebRestListener;
import be.nabu.glue.MultipleRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.parsers.GlueParserProvider;
import be.nabu.glue.repositories.ScannableScriptRepository;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.ContentRewriter;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.ResourceHandler;
import be.nabu.libs.http.server.SessionProviderImpl;
import be.nabu.libs.http.server.util.CSSMerger;
import be.nabu.libs.http.server.util.JavascriptMerger;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.EmptyServiceRuntimeTracker;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.utils.io.IOUtils;

/**
 * TODO: integrate session provider to use same cache as service cache 
 */
public class WebArtifact extends JAXBArtifact<WebArtifactConfiguration> implements StartableArtifact, StoppableArtifact {

	private List<EventSubscription<HTTPRequest, HTTPResponse>> subscriptions = new ArrayList<EventSubscription<HTTPRequest, HTTPResponse>>();
	private GlueListener listener;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private Repository repository;
	
	public WebArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, "webartifact.xml", WebArtifactConfiguration.class);
		this.repository = repository;
	}

	@Override
	public void stop() throws IOException {
		for (EventSubscription<HTTPRequest, HTTPResponse> subscription : subscriptions) {
			subscription.unsubscribe();
		}
		subscriptions.clear();
	}

	@Override
	public void start() throws IOException {
		boolean isDevelopment = EAIResourceRepository.isDevelopment();
		if (subscriptions.isEmpty()) {
			getConfiguration();
			String serverPath = getConfiguration().getPath();
			if (serverPath == null) {
				serverPath = "/";
			}
			else if (!serverPath.startsWith("/")) {
				serverPath = "/" + serverPath;
			}
			
			// build repository
			MultipleRepository repository = new MultipleRepository(null);
						
			ResourceContainer<?> publicDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PUBLIC);
			List<ContentRewriter> rewriters = new ArrayList<ContentRewriter>();
			HTTPServer server = getConfiguration().getHttpServer().getServer();
			boolean hasPages = false;
			if (publicDirectory != null) {
				// check if there is a resource directory
				ResourceContainer<?> resources = (ResourceContainer<?>) publicDirectory.getChild("resources");
				if (resources != null) {
					String resourcePath = serverPath.equals("/") ? "/resources" : serverPath + "/resources";
					ResourceHandler handler = new ResourceHandler(resources, resourcePath, !isDevelopment);
					EventSubscription<HTTPRequest, HTTPResponse> subscription = server.getEventDispatcher().subscribe(HTTPRequest.class, handler);
					subscription.filter(HTTPServerUtils.limitToPath(resourcePath));
					subscriptions.add(subscription);
					// add optimizations if it is not development
					if (!isDevelopment) {
						JavascriptMerger javascriptMerger = new JavascriptMerger(resources, resourcePath);
						EventSubscription<HTTPRequest, HTTPResponse> javascriptMergerSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, javascriptMerger);
						subscriptions.add(javascriptMergerSubscription);
						javascriptMergerSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
						rewriters.add(javascriptMerger);
						
						CSSMerger cssMerger = new CSSMerger(resources, resourcePath);
						EventSubscription<HTTPRequest, HTTPResponse> cssMergerSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, cssMerger);
						subscriptions.add(cssMergerSubscription);
						cssMergerSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
						rewriters.add(cssMerger);
					}
				}
				ResourceContainer<?> pages = (ResourceContainer<?>) publicDirectory.getChild("pages");
				if (pages != null) {
					hasPages = true;
					// the configured charset is for the end user, NOT for the local glue scripts, that should be the system default
					ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, pages, new GlueParserProvider(), Charset.defaultCharset());
					scannableScriptRepository.setGroup(GlueListener.PUBLIC);
					repository.add(scannableScriptRepository);
				}
			}
			ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
			// the private directory houses the scripts
			if (privateDirectory != null) {
				// currently only a scripts folder, but we may want to add more private folders later on
				ResourceContainer<?> scripts = (ResourceContainer<?>) privateDirectory.getChild("scripts");
				if (scripts != null) {
					repository.add(new ScannableScriptRepository(repository, scripts, new GlueParserProvider(), Charset.defaultCharset()));
				}
			}
			// only set up a glue listener if there are any public pages
			SessionProvider sessionProvider = new SessionProviderImpl(1000*60*30);
			if (hasPages) {
				Properties properties = new Properties();
				if (getDirectory().getChild(".properties") instanceof ReadableResource) {
					InputStream input = IOUtils.toInputStream(new ResourceReadableContainer((ReadableResource) getDirectory().getChild(".properties")));
					try {
						properties.load(input);
					}
					finally {
						input.close();
					}
				}
				
				Map<String, String> environment = new HashMap<String, String>();
				if (!properties.isEmpty()) {
					for (Object key : properties.keySet()) {
						if (key == null) {
							continue;
						}
						environment.put(key.toString().trim(), properties.getProperty(key.toString()).trim());
					}
				}
				
				String environmentName = serverPath;
				if (environmentName.startsWith("/")) {
					environmentName.substring(1);
				}
				if (environmentName.isEmpty()) {
					environmentName = "root";
				}
				
				listener = new GlueListener(
					sessionProvider, 
					repository, 
					new SimpleExecutionEnvironment(environmentName, environment),
					serverPath
				);
				listener.getContentRewriters().addAll(rewriters);
				listener.setRefreshScripts(isDevelopment);
				listener.setAllowEncoding(!isDevelopment);
				listener.setAuthenticator(getAuthenticator());
				listener.setTokenValidator(getTokenValidator());
				listener.setPermissionHandler(getPermissionHandler());
				listener.setRoleHandler(getRoleHandler());
				listener.setRealm(getId());
				EventSubscription<HTTPRequest, HTTPResponse> subscription = server.getEventDispatcher().subscribe(HTTPRequest.class, listener);
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
			}
			// load any registered rest services
			List<DefinedService> restServices = getConfiguration().getRestServices();
			if (restServices != null) {
				for (DefinedService service : restServices) {
					ServiceInterface serviceInterface = service.getServiceInterface();
					while (!(serviceInterface instanceof WebRestArtifact)) {
						serviceInterface = serviceInterface.getParent();
						if (serviceInterface == null) {
							logger.error("Can not start service '" + service.getId() + "' because it does not implement a REST interface");
							break;
						}
					}
					if (serviceInterface instanceof WebRestArtifact) {
						WebRestListener listener = new WebRestListener(
							this.repository, 
							serverPath, 
							getId(), 
							sessionProvider, 
							getPermissionHandler(), 
							getRoleHandler(), 
							getTokenValidator(), 
							((WebRestArtifact) serviceInterface), 
							service, 
							Charset.forName(getConfiguration().getCharset()), 
							!isDevelopment
						);
						EventSubscription<HTTPRequest, HTTPResponse> subscription = server.getEventDispatcher().subscribe(HTTPRequest.class, listener);
						subscription.filter(HTTPServerUtils.limitToPath(serverPath));
						subscriptions.add(subscription);
					}
				}
			}
		}
	}
	
	public Authenticator getAuthenticator() throws IOException {
		if (getConfiguration().getAuthenticationService() != null) {
			return POJOUtils.newProxy(Authenticator.class, getConfiguration().getAuthenticationService(), new EmptyServiceRuntimeTracker());
		}
		return null;
	}
	public RoleHandler getRoleHandler() throws IOException {
		if (getConfiguration().getRoleService() != null) {
			return POJOUtils.newProxy(RoleHandler.class, getConfiguration().getRoleService(), new EmptyServiceRuntimeTracker());
		}
		return null;
	}
	public PermissionHandler getPermissionHandler() throws IOException {
		if (getConfiguration().getPermissionService() != null) {
			return POJOUtils.newProxy(PermissionHandler.class, getConfiguration().getPermissionService(), new EmptyServiceRuntimeTracker());
		}
		return null;
	}
	public TokenValidator getTokenValidator() throws IOException {
		if (getConfiguration().getTokenValidatorService() != null) {
			return POJOUtils.newProxy(TokenValidator.class, getConfiguration().getTokenValidatorService(), new EmptyServiceRuntimeTracker());
		}
		return null;
	}

	public GlueListener getListener() {
		return listener;
	}

}

package be.nabu.eai.repository.artifacts.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.http.RepositoryExceptionFormatter;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.artifacts.web.WebArtifactDebugger.AnyThreadTracker;
import be.nabu.eai.repository.artifacts.web.rest.WebRestArtifact;
import be.nabu.eai.repository.artifacts.web.rest.WebRestListener;
import be.nabu.eai.repository.util.CombinedAuthenticator;
import be.nabu.eai.repository.util.FlatServiceTrackerWrapper;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.eai.services.api.FlatServiceTracker;
import be.nabu.glue.MultipleRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.parsers.GlueParserProvider;
import be.nabu.glue.repositories.ScannableScriptRepository;
import be.nabu.glue.services.ServiceMethodProvider;
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
import be.nabu.libs.http.server.BasicAuthenticationHandler;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.ResourceHandler;
import be.nabu.libs.http.server.SessionProviderImpl;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.EmptyServiceRuntimeTracker;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.services.vm.MultipleVMServiceRuntimeTracker;
import be.nabu.utils.io.IOUtils;

/**
 * TODO: integrate session provider to use same cache as service cache
 */
public class WebArtifact extends JAXBArtifact<WebArtifactConfiguration> implements StartableArtifact, StoppableArtifact {

	private List<EventSubscription<?, ?>> subscriptions = new ArrayList<EventSubscription<?, ?>>();
	private GlueListener listener;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private Repository repository;
	
	public WebArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, "webartifact.xml", WebArtifactConfiguration.class);
		this.repository = repository;
	}

	@Override
	public void stop() throws IOException {
		logger.info("Stopping " + subscriptions.size() + " subscriptions");
		for (EventSubscription<?, ?> subscription : subscriptions) {
			subscription.unsubscribe();
		}
		subscriptions.clear();
		// unregister codes
		HTTPServer server = getConfiguration().getHttpServer().getServer();
		if (server != null && server.getExceptionFormatter() instanceof RepositoryExceptionFormatter) {
			((RepositoryExceptionFormatter) server.getExceptionFormatter()).unregister(getId());
		}
	}

	@Override
	public void start() throws IOException {
		boolean isDevelopment = EAIResourceRepository.isDevelopment();
		if (subscriptions.isEmpty()) {
			String realm = getConfiguration().getRealm() == null ? getId() : getConfiguration().getRealm();
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
			if (server.getExceptionFormatter() instanceof RepositoryExceptionFormatter && getConfiguration().getWhitelistedCodes() != null) {
				((RepositoryExceptionFormatter) server.getExceptionFormatter()).register(getId(), Arrays.asList(getConfiguration().getWhitelistedCodes().split("[\\s]*,[\\s]*")));
			}
			
			// set up a basic authentication listener which optionally interprets that, it allows for REST-based access
			if (getConfiguration().getAllowBasicAuthentication() != null && getConfiguration().getAllowBasicAuthentication()) {
				BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler(getAuthenticator(), HTTPServerUtils.newFixedRealmHandler(realm));
				// make sure it is not mandatory
				basicAuthenticationHandler.setRequired(false);
				EventSubscription<HTTPRequest, HTTPResponse> authenticationSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, basicAuthenticationHandler);
				authenticationSubscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(authenticationSubscription);
				
				// for all responses, we check a 401 to see if it has the required WWW-Authenticate header
				EventSubscription<HTTPResponse, HTTPResponse> ensureAuthenticationSubscription = server.getEventDispatcher().subscribe(HTTPResponse.class, HTTPServerUtils.ensureAuthenticateHeader(realm));
				subscriptions.add(ensureAuthenticationSubscription);
			}
			
			// only set up a glue listener if there are any public pages
			SessionProvider sessionProvider = new SessionProviderImpl(1000*60*30);
			
			WebArtifactDebugger debugger = null;
			if (isDevelopment) {
				debugger = new WebArtifactDebugger(serverPath, sessionProvider);
				// the request listener
				EventSubscription<HTTPRequest, HTTPResponse> subscription = server.getEventDispatcher().subscribe(HTTPRequest.class, debugger.getRequestListener());
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
				// the response listener
				subscriptions.add(server.getEventDispatcher().subscribe(HTTPResponse.class, debugger.getResponseListener()));
			}
			boolean hasPages = false;
			ServiceMethodProvider serviceMethodProvider = new ServiceMethodProvider(this.repository, this.repository, new AnyThreadTracker());
			if (publicDirectory != null) {
				// check if there is a resource directory
				ResourceContainer<?> resources = (ResourceContainer<?>) publicDirectory.getChild("resources");
				if (resources != null) {
					logger.debug("Adding resource listener for folder: " + resources);
					String resourcePath = serverPath.equals("/") ? "/resources" : serverPath + "/resources";
					ResourceHandler handler = new ResourceHandler(resources, resourcePath, !isDevelopment);
					EventSubscription<HTTPRequest, HTTPResponse> subscription = server.getEventDispatcher().subscribe(HTTPRequest.class, handler);
					subscription.filter(HTTPServerUtils.limitToPath(resourcePath));
					subscriptions.add(subscription);
					// add optimizations if it is not development
					if (!isDevelopment) {
//						logger.debug("Adding javascript merger");
//						JavascriptMerger javascriptMerger = new JavascriptMerger(resources, resourcePath);
//						EventSubscription<HTTPRequest, HTTPResponse> javascriptMergerSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, javascriptMerger);
//						subscriptions.add(javascriptMergerSubscription);
//						javascriptMergerSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
//						rewriters.add(javascriptMerger);
//						
//						logger.debug("Adding css merger");
//						CSSMerger cssMerger = new CSSMerger(resources, resourcePath);
//						EventSubscription<HTTPRequest, HTTPResponse> cssMergerSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, cssMerger);
//						subscriptions.add(cssMergerSubscription);
//						cssMergerSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
//						rewriters.add(cssMerger);
					}
				}
				ResourceContainer<?> pages = (ResourceContainer<?>) publicDirectory.getChild("pages");
				if (pages != null) {
					logger.debug("Adding public scripts found in: " + pages);
					hasPages = true;
					// the configured charset is for the end user, NOT for the local glue scripts, that should be the system default
					ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, pages, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset());
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
					logger.debug("Adding private scripts found in: " + scripts);
					repository.add(new ScannableScriptRepository(repository, scripts, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset()));
				}
			}
			if (hasPages) {
				Properties properties = new Properties();
				if (getDirectory().getChild(".properties") instanceof ReadableResource) {
					logger.debug("Adding properties found in: " + getDirectory().getChild(".properties"));
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
				if (isDevelopment) {
					environment.put("development", "true");
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
				listener.setRealm(realm);
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
						logger.debug("Adding rest handler for service: " + service.getId());
						WebRestListener listener = new WebRestListener(
							getServiceTracker(),
							this.repository, 
							serverPath, 
							realm, 
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
			logger.info("Started " + subscriptions.size() + " subscriptions");
		}
	}
	
	public ServiceRuntimeTracker getServiceTracker() throws IOException {//FlowServiceTracker
		List<ServiceRuntimeTracker> trackers = new ArrayList<ServiceRuntimeTracker>();
		if (EAIResourceRepository.isDevelopment()) {
			trackers.add(new AnyThreadTracker());
			if (getConfiguration().getTrackerService() != null) {
				FlatServiceTracker flatServiceTracker = POJOUtils.newProxy(FlatServiceTracker.class, getConfiguration().getTrackerService(), new AnyThreadTracker(), repository, SystemPrincipal.ROOT);
				trackers.add(new FlatServiceTrackerWrapper(flatServiceTracker));
			}
		}
		else if (getConfiguration().getTrackerService() != null) {
			FlatServiceTracker flatServiceTracker = POJOUtils.newProxy(FlatServiceTracker.class, getConfiguration().getTrackerService(), new EmptyServiceRuntimeTracker(), repository, SystemPrincipal.ROOT);
			trackers.add(new FlatServiceTrackerWrapper(flatServiceTracker));
		}
		return new MultipleVMServiceRuntimeTracker(trackers.toArray(new ServiceRuntimeTracker[trackers.size()]));
	}
	
	public Authenticator getAuthenticator() throws IOException {
		PasswordAuthenticator passwordAuthenticator = null;
		if (getConfiguration().getPasswordAuthenticationService() != null) {
			passwordAuthenticator = POJOUtils.newProxy(PasswordAuthenticator.class, getConfiguration().getPasswordAuthenticationService(), getServiceTracker(), EAIResourceRepository.getInstance(), SystemPrincipal.ROOT);
		}
		SecretAuthenticator sharedSecretAuthenticator = null;
		if (getConfiguration().getSecretAuthenticationService() != null) {
			sharedSecretAuthenticator = POJOUtils.newProxy(SecretAuthenticator.class, getConfiguration().getSecretAuthenticationService(), getServiceTracker(), EAIResourceRepository.getInstance(), SystemPrincipal.ROOT);
		}
		return new CombinedAuthenticator(passwordAuthenticator, sharedSecretAuthenticator);
	}
	public RoleHandler getRoleHandler() throws IOException {
		if (getConfiguration().getRoleService() != null) {
			return POJOUtils.newProxy(RoleHandler.class, getConfiguration().getRoleService(), getServiceTracker(), EAIResourceRepository.getInstance(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public PermissionHandler getPermissionHandler() throws IOException {
		if (getConfiguration().getPermissionService() != null) {
			return POJOUtils.newProxy(PermissionHandler.class, getConfiguration().getPermissionService(), getServiceTracker(), EAIResourceRepository.getInstance(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public TokenValidator getTokenValidator() throws IOException {
		if (getConfiguration().getTokenValidatorService() != null) {
			return POJOUtils.newProxy(TokenValidator.class, getConfiguration().getTokenValidatorService(), getServiceTracker(), EAIResourceRepository.getInstance(), SystemPrincipal.ROOT);
		}
		return null;
	}

	public GlueListener getListener() {
		return listener;
	}

}

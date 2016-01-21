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
import be.nabu.eai.repository.MetricsLevelProvider;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.UserLanguageProvider;
import be.nabu.eai.repository.artifacts.http.RepositoryExceptionFormatter;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.impl.CacheSessionProvider;
import be.nabu.eai.repository.util.CombinedAuthenticator;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.glue.MultipleRepository;
import be.nabu.glue.ScriptRuntime;
import be.nabu.glue.ScriptUtils;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.api.StringSubstituter;
import be.nabu.glue.api.StringSubstituterProvider;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.parsers.GlueParserProvider;
import be.nabu.glue.repositories.ScannableScriptRepository;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.impl.AccessBasedTimeoutManager;
import be.nabu.libs.cache.impl.SerializableSerializer;
import be.nabu.libs.cache.impl.StringSerializer;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.events.filters.AndEventFilter;
import be.nabu.libs.http.api.ContentRewriter;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.http.api.server.SessionResolver;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GluePostProcessListener;
import be.nabu.libs.http.glue.GluePreprocessListener;
import be.nabu.libs.http.glue.GlueSessionResolver;
import be.nabu.libs.http.glue.impl.RequestMethods;
import be.nabu.libs.http.glue.impl.UserMethods;
import be.nabu.libs.http.server.BasicAuthenticationHandler;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.ResourceHandler;
import be.nabu.libs.http.server.SessionProviderImpl;
import be.nabu.libs.metrics.api.GroupLevelProvider;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.metrics.core.MetricInstanceImpl;
import be.nabu.libs.metrics.core.api.SinkEvent;
import be.nabu.libs.metrics.core.filters.ThresholdOverTimeFilter;
import be.nabu.libs.metrics.impl.MetricGrouper;
import be.nabu.libs.resources.CombinedContainer;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.impl.MimeUtils;

/**
 * TODO: integrate session provider to use same cache as service cache
 * Maybe re-add "AnyThreadTracker" to the context?
 */
public class WebArtifact extends JAXBArtifact<WebArtifactConfiguration> implements StartableArtifact, StoppableArtifact {

	private Map<ResourceContainer<?>, ScriptRepository> additionalRepositories = new HashMap<ResourceContainer<?>, ScriptRepository>();
	private List<EventSubscription<?, ?>> subscriptions = new ArrayList<EventSubscription<?, ?>>();
	private GlueListener listener;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private SessionProvider sessionProvider;
	private boolean started;
	private MultipleRepository repository;
	private ServiceMethodProvider serviceMethodProvider;
	private ResourceHandler resourceHandler;
	
	public WebArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "webartifact.xml", WebArtifactConfiguration.class);
	}

	@Override
	public void stop() throws IOException {
		started = false;
		sessionProvider = null;
		logger.info("Stopping " + subscriptions.size() + " subscriptions");
		for (EventSubscription<?, ?> subscription : subscriptions) {
			subscription.unsubscribe();
		}
		subscriptions.clear();
		// unregister codes
		HTTPServer server = getConfiguration().getVirtualHost().getConfiguration().getServer().getServer();
		if (server != null && server.getExceptionFormatter() instanceof RepositoryExceptionFormatter) {
			((RepositoryExceptionFormatter) server.getExceptionFormatter()).unregister(getId());
		}
		if (getConfiguration().getCacheProvider() != null) {
			getConfiguration().getCacheProvider().remove(getId());
		}
		List<WebFragment> webFragments = getConfiguration().getWebFragments();
		if (webFragments != null) {
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					fragment.stop(this, null);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void start() throws IOException {
		boolean isDevelopment = EAIResourceRepository.isDevelopment();
		if (!started && getConfiguration().getVirtualHost() != null) {
			String realm = getRealm();
			String serverPath = getServerPath();

			ResourceContainer<?> publicDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PUBLIC);
			ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
			
			serviceMethodProvider = new ServiceMethodProvider(getRepository(), getRepository());
			
			ResourceContainer<?> meta = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("meta");
			ScriptRepository metaRepository = null;
			if (meta != null) {
				metaRepository = new ScannableScriptRepository(null, meta, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset());
			}
			
			repository = new MultipleRepository(null);
			List<ContentRewriter> rewriters = new ArrayList<ContentRewriter>();
			HTTPServer server = getConfiguration().getVirtualHost().getConfiguration().getServer().getServer();
			if (server.getExceptionFormatter() instanceof RepositoryExceptionFormatter && getConfiguration().getWhitelistedCodes() != null) {
				((RepositoryExceptionFormatter) server.getExceptionFormatter()).register(getId(), Arrays.asList(getConfiguration().getWhitelistedCodes().split("[\\s]*,[\\s]*")));
			}

			// create session provider
			if (getConfiguration().getCacheProvider() != null) {
				Cache sessionCache = getConfiguration().getCacheProvider().create(
					getId(),
					// defaults to unlimited
					getConfiguration().getMaxTotalSessionSize() == null ? 0 : getConfiguration().getMaxTotalSessionSize(),
					// defaults to unlimited
					getConfiguration().getMaxSessionSize() == null ? 0 : getConfiguration().getMaxSessionSize(),
					new StringSerializer(),
					new SerializableSerializer(),
					// no refreshing logic obviously
					null,
					// defaults to 1 hour
					new AccessBasedTimeoutManager(getConfiguration().getSessionTimeout() == null ? 1000l*60*60 : getConfiguration().getSessionTimeout())
				);
				sessionProvider = new CacheSessionProvider(sessionCache);
			}
			else {
				sessionProvider = new SessionProviderImpl(getConfiguration().getSessionTimeout() == null ? 1000l*60*60 : getConfiguration().getSessionTimeout());
			}
			
			Map<String, String> environment = getProperties();
			if (isDevelopment) {
				environment.put("development", "true");
			}
			// always set the id of the web artifact (need it to introspect artifact)
			environment.put("webArtifactId", getId());
			
			String environmentName = serverPath;
			if (environmentName.startsWith("/")) {
				environmentName.substring(1);
			}
			if (environmentName.isEmpty()) {
				environmentName = "root";
			}
			
			EventDispatcher dispatcher = getConfiguration().getVirtualHost().getDispatcher();
			// before the base authentication required authenticate header rewriter, add a rewriter for the response (if applicable)
			if (metaRepository != null) {
				GluePostProcessListener postprocessListener = new GluePostProcessListener(
					metaRepository, 
					new SimpleExecutionEnvironment(environmentName, environment),
					serverPath
				);
				postprocessListener.setRefresh(isDevelopment);
				postprocessListener.setRealm(realm);
				EventSubscription<HTTPResponse, HTTPResponse> subscription = dispatcher.subscribe(HTTPResponse.class, postprocessListener);
				subscription.filter(HTTPServerUtils.limitToRequestPath(serverPath));
				subscriptions.add(subscription);
			}
			
			// set up a basic authentication listener which optionally interprets that, it allows for REST-based access
			Authenticator authenticator = getAuthenticator();
			if (getConfiguration().getAllowBasicAuthentication() != null && getConfiguration().getAllowBasicAuthentication()) {
				BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler(authenticator, HTTPServerUtils.newFixedRealmHandler(realm));
				// make sure it is not mandatory
				basicAuthenticationHandler.setRequired(false);
				EventSubscription<HTTPRequest, HTTPResponse> authenticationSubscription = dispatcher.subscribe(HTTPRequest.class, basicAuthenticationHandler);
				authenticationSubscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(authenticationSubscription);
				
				// for all responses, we check a 401 to see if it has the required WWW-Authenticate header
				// in retrospect: don't add it? (maybe configurable?)
				// problem is it pops up a window in the browser to authenticate
//				EventSubscription<HTTPResponse, HTTPResponse> ensureAuthenticationSubscription = dispatcher.subscribe(HTTPResponse.class, HTTPServerUtils.ensureAuthenticateHeader(realm));
//				ensureAuthenticationSubscription.filter(HTTPServerUtils.limitToRequestPath(serverPath));
//				subscriptions.add(ensureAuthenticationSubscription);
			}
			
			// after the base authentication but before anything else, allow for rewriting
			if (metaRepository != null) {
				GluePreprocessListener preprocessListener = new GluePreprocessListener(
					authenticator,
					sessionProvider, 
					metaRepository, 
					new SimpleExecutionEnvironment(environmentName, environment),
					serverPath
				);
				preprocessListener.setRefresh(isDevelopment);
				preprocessListener.setTokenValidator(getTokenValidator());
				preprocessListener.setRealm(realm);
				EventSubscription<HTTPRequest, HTTPRequest> subscription = dispatcher.subscribe(HTTPRequest.class, preprocessListener);
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
			}
			
			WebArtifactDebugger debugger = null;
			if (isDevelopment) {
				debugger = new WebArtifactDebugger(serverPath, sessionProvider);
				// the request listener
				EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, debugger.getRequestListener());
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
				// the response listener
				subscriptions.add(dispatcher.subscribe(HTTPResponse.class, debugger.getResponseListener()));
			}
			ResourceContainer<?> resources = null;
			if (publicDirectory != null) {
				// check if there is a resource directory
				resources = (ResourceContainer<?>) publicDirectory.getChild("resources");
				if (resources != null) {
					logger.debug("Adding resource listener for folder: " + resources);
					if (isDevelopment && privateDirectory != null) {
						Resource child = privateDirectory.getChild("resources");
						if (child != null) {
							resources = new CombinedContainer(null, "resources", resources, (ResourceContainer<?>) child);
						}
					}
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
					// the configured charset is for the end user, NOT for the local glue scripts, that should be the system default
					ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, pages, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset());
					scannableScriptRepository.setGroup(GlueListener.PUBLIC);
					repository.add(scannableScriptRepository);
				}
			}

			String resourcePath = serverPath.equals("/") ? "/resources" : serverPath + "/resources";
			resourceHandler = new ResourceHandler(resources, resourcePath, !isDevelopment);
			EventSubscription<HTTPRequest, HTTPResponse> resourceSubscription = dispatcher.subscribe(HTTPRequest.class, resourceHandler);
			resourceSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
			subscriptions.add(resourceSubscription);

			// the private directory houses the scripts
			if (privateDirectory != null) {
				// currently only a scripts folder, but we may want to add more private folders later on
				ResourceContainer<?> scripts = (ResourceContainer<?>) privateDirectory.getChild("scripts");
				if (scripts != null) {
					logger.debug("Adding private scripts found in: " + scripts);
					repository.add(new ScannableScriptRepository(repository, scripts, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset()));
				}
			}
			listener = new GlueListener(
				sessionProvider, 
				repository, 
				new SimpleExecutionEnvironment(environmentName, environment),
				serverPath
			);
			
			if (getConfiguration().getFailedLoginThreshold() != null) {
				MetricInstance metricInstance = getRepository().getMetricInstance(getId());
				// we need to up the grouping level of the login failed, otherwise we never get to the ip level
				if (metricInstance instanceof MetricGrouper) {
					GroupLevelProvider groupLevelProvider = ((MetricGrouper) metricInstance).getGroupLevelProvider();
					if (groupLevelProvider instanceof MetricsLevelProvider) {
						((MetricsLevelProvider) groupLevelProvider).set(UserMethods.METRICS_LOGIN_FAILED, 2);
					}
					else {
						throw new IllegalArgumentException("Expecting a MetricsLevelProvider");	
					}
					// add an event dispatcher to trigger on failed logins and blacklist an ip
					MetricInstance parent = ((MetricGrouper) metricInstance).getParent();
					if (parent instanceof MetricInstanceImpl) {
						EventDispatcher metricsDispatcher = ((MetricInstanceImpl) parent).getDispatcher();
						if (metricsDispatcher != null) {
							// by default the user will be blocked for 15 minutes
							EventSubscription<SinkEvent, Void> metricsSubscription = metricsDispatcher.subscribe(SinkEvent.class, new FailedLoginListener(this, getConfiguration().getFailedLoginBlacklistDuration() == null ? 15 * 60000l : getConfiguration().getFailedLoginBlacklistDuration()));
							metricsSubscription.filter(new AndEventFilter<SinkEvent>(
								new EventHandler<SinkEvent, Boolean>() {
									@Override
									public Boolean handle(SinkEvent event) {
										return event.getCategory().startsWith(UserMethods.METRICS_LOGIN_FAILED + ":");
									}
								},
								// the default window is 10 minutes
								new ThresholdOverTimeFilter(getConfiguration().getFailedLoginThreshold(), true, getConfiguration().getFailedLoginWindow() == null ? 600000l : getConfiguration().getFailedLoginWindow())
							));
						}
					}
					else {
						throw new IllegalArgumentException("Expecting a MetricInstanceImpl");
					}
				}
				else {
					throw new IllegalArgumentException("Expecting a MetricGrouper");
				}
				listener.setMetrics(metricInstance);
			}
			
			if (getConfiguration().getTranslationService() != null) {
				final UserLanguageProvider languageProvider = getLanguageProvider();
				listener.getSubstituterProviders().add(new StringSubstituterProvider() {
					@Override
					public StringSubstituter getSubstituter(ScriptRuntime runtime) {
						String language = null;
						// if we have a language provider, try to get it from there
						if (languageProvider != null) {
							Token token = UserMethods.token();
							// the token may be null then the provider can send back a default language
							language = languageProvider.getLanguage(token);
						}
						// if there is no language provider or it does not have a language for the user, try to detect from browser settings
						if (language == null) {
							if (RequestMethods.content().getContent() != null) {
								List<String> acceptedLanguages = MimeUtils.getAcceptedLanguages(RequestMethods.content().getContent().getHeaders());
								if (!acceptedLanguages.isEmpty()) {
									language = acceptedLanguages.get(0).replaceAll("-.*$", "");
								}
							}
						}
						try {
							return new be.nabu.glue.impl.ImperativeSubstitutor("%", "string(" + getConfiguration().getTranslationService().getId() + "('" + ScriptUtils.getFullName(runtime.getScript()) + "', '${value}', " + (language == null ? "null" : "'" + language + "'") + ")/translation)");
						}
						catch (IOException e) {
							throw new RuntimeException("Could not get translation service", e);
						}
					}
				});
			}
			listener.getContentRewriters().addAll(rewriters);
			listener.setRefreshScripts(isDevelopment);
			listener.setAllowEncoding(!isDevelopment);
			listener.setAuthenticator(authenticator);
			listener.setTokenValidator(getTokenValidator());
			listener.setPermissionHandler(getPermissionHandler());
			listener.setRoleHandler(getRoleHandler());
			listener.setRealm(realm);
			// always creating a session can create other issues
//			listener.setAlwaysCreateSession(true);
			EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, listener);
			subscription.filter(HTTPServerUtils.limitToPath(serverPath));
			subscriptions.add(subscription);
			List<WebFragment> webFragments = getConfiguration().getWebFragments();
			if (webFragments != null) {
				for (WebFragment fragment : webFragments) {
					if (fragment != null) {
						fragment.start(this, null);
					}
				}
			}
			started = true;
			logger.info("Started " + subscriptions.size() + " subscriptions");
		}
	}
	
	public Map<String, String> getProperties() throws IOException {
		// load properties
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
		return environment;
	}

	public String getRealm() throws IOException {
		return getConfiguration().getRealm() == null ? getId() : getConfiguration().getRealm();
	}

	public String getServerPath() throws IOException {
		String serverPath = getConfiguration().getPath();
		if (serverPath == null) {
			serverPath = "/";
		}
		else if (!serverPath.startsWith("/")) {
			serverPath = "/" + serverPath;
		}
		return serverPath;
	}
	
	public Authenticator getAuthenticator() throws IOException {
		PasswordAuthenticator passwordAuthenticator = null;
		if (getConfiguration().getPasswordAuthenticationService() != null) {
			passwordAuthenticator = POJOUtils.newProxy(PasswordAuthenticator.class, getConfiguration().getPasswordAuthenticationService(), getRepository(), SystemPrincipal.ROOT);
		}
		SecretAuthenticator sharedSecretAuthenticator = null;
		if (getConfiguration().getSecretAuthenticationService() != null) {
			sharedSecretAuthenticator = POJOUtils.newProxy(SecretAuthenticator.class, getConfiguration().getSecretAuthenticationService(), getRepository(), SystemPrincipal.ROOT);
		}
		return new CombinedAuthenticator(passwordAuthenticator, sharedSecretAuthenticator);
	}
	public RoleHandler getRoleHandler() throws IOException {
		if (getConfiguration().getRoleService() != null) {
			return POJOUtils.newProxy(RoleHandler.class, getConfiguration().getRoleService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public PermissionHandler getPermissionHandler() throws IOException {
		if (getConfiguration().getPermissionService() != null) {
			return POJOUtils.newProxy(PermissionHandler.class, getConfiguration().getPermissionService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public TokenValidator getTokenValidator() throws IOException {
		if (getConfiguration().getTokenValidatorService() != null) {
			return POJOUtils.newProxy(TokenValidator.class, getConfiguration().getTokenValidatorService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}
	public UserLanguageProvider getLanguageProvider() throws IOException {
		if (getConfiguration().getLanguageProviderService() != null) {
			return POJOUtils.newProxy(UserLanguageProvider.class, getConfiguration().getLanguageProviderService(), getRepository(), SystemPrincipal.ROOT);
		}
		return null;
	}
	
	public GlueListener getListener() {
		return listener;
	}

	@Override
	public boolean isStarted() {
		return !subscriptions.isEmpty();
	}

	public SessionProvider getSessionProvider() {
		return sessionProvider;
	}

	public SessionResolver getSessionResolver() {
		return new GlueSessionResolver(sessionProvider);
	}

	public EventDispatcher getDispatcher() {
		try {
			return getConfiguration().getVirtualHost() != null ? getConfiguration().getVirtualHost().getDispatcher() : null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void addGlueScripts(ResourceContainer<?> parent, boolean isPublic) throws IOException {
		if (repository != null) {
			ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, parent, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset());
			if (isPublic) {
				scannableScriptRepository.setGroup(GlueListener.PUBLIC);
			}
			additionalRepositories.put(parent, scannableScriptRepository);
			repository.add(scannableScriptRepository);
		}
	}
	public void removeGlueScripts(ResourceContainer<?> parent) {
		if (repository != null && additionalRepositories.containsKey(parent)) {
			repository.remove(additionalRepositories.get(parent));
			additionalRepositories.remove(parent);
		}
	}
	
	public void addResources(ResourceContainer<?> parent) {
		if (resourceHandler != null) {
			resourceHandler.addRoot(parent);
		}
	}
	public void removeResources(ResourceContainer<?> parent) {
		if (resourceHandler != null) {
			resourceHandler.removeRoot(parent);
		}
	}
	
	public ResourceHandler getResourceHandler() {
		return resourceHandler;
	}
}

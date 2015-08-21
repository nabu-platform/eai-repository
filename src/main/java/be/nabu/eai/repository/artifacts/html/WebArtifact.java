package be.nabu.eai.repository.artifacts.html;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.parsers.GlueParserProvider;
import be.nabu.glue.repositories.ScannableScriptRepository;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.ResourceHandler;
import be.nabu.libs.http.server.SessionProviderImpl;
import be.nabu.libs.resources.api.ResourceContainer;

/**
 * TODO: integrate session provider to use same cache as service cache 
 */
public class WebArtifact extends JAXBArtifact<WebArtifactConfiguration> implements StartableArtifact, StoppableArtifact {

	private List<EventSubscription<HTTPRequest, HTTPResponse>> subscriptions = new ArrayList<EventSubscription<HTTPRequest, HTTPResponse>>();
	
	public WebArtifact(String id, ResourceContainer<?> directory) {
		super(id, directory, "webartifact.xml", WebArtifactConfiguration.class);
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
		if (subscriptions.isEmpty()) {
			getConfiguration();
			String path = getConfiguration().getPath();
			// TODO: add a security handler that ties into the EAI platform permissions!
			if (path == null) {
				path = "/";
			}
			else if (!path.startsWith("/")) {
				path = "/" + path;
			}
			ResourceContainer<?> publicDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PUBLIC);
			// the public directory houses the resources
			if (publicDirectory != null) {
				String resourcePath = path.equals("/") ? "/resources" : path + "/resources";
				ResourceHandler handler = new ResourceHandler(publicDirectory, resourcePath, !EAIResourceRepository.isDevelopment());
				EventSubscription<HTTPRequest, HTTPResponse> subscription = getConfiguration().getHttpServer().getServer().getEventDispatcher().subscribe(HTTPRequest.class, handler);
				subscription.filter(HTTPServerUtils.limitToPath(resourcePath));
				subscriptions.add(subscription);
			}
			ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
			// the private directory houses the scripts
			if (privateDirectory != null) {
				GlueListener listener = new GlueListener(
					new SessionProviderImpl(1000*60*30), 
					new ScannableScriptRepository(null, privateDirectory, new GlueParserProvider(new ServiceMethodProvider(EAIResourceRepository.getInstance(), null)), getConfiguration().getCharset() == null ? Charset.forName("UTF-8") : Charset.forName(getConfiguration().getCharset()), true), 
					new SimpleExecutionEnvironment("local"),
					path
				);
				System.out.println("IS DEVELOPMENT: " + EAIResourceRepository.isDevelopment());
				listener.setRefreshScripts(EAIResourceRepository.isDevelopment());
				EventSubscription<HTTPRequest, HTTPResponse> subscription = getConfiguration().getHttpServer().getServer().getEventDispatcher().subscribe(HTTPRequest.class, listener);
				subscription.filter(HTTPServerUtils.limitToPath(path));
				subscriptions.add(subscription);
			}
		}
	}

}

package be.nabu.eai.repository.artifacts.html;

import java.io.IOException;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.http.api.HTTPRequest;
import be.nabu.utils.http.api.HTTPResponse;
import be.nabu.utils.http.server.PathFilter;
import be.nabu.utils.http.server.ResourceHandler;

public class WebArtifact extends JAXBArtifact<WebArtifactConfiguration> implements StartableArtifact, StoppableArtifact {

	private EventSubscription<HTTPRequest, HTTPResponse> subscription;
	
	public WebArtifact(String id, ResourceContainer<?> directory) {
		super(id, directory, "webartifact.xml", WebArtifactConfiguration.class);
	}

	@Override
	public void stop() throws IOException {
		if (subscription != null) {
			subscription.unsubscribe();
			subscription = null;
		}
	}

	@Override
	public void start() throws IOException {
		System.out.println("Starting 1...");
		if (subscription == null) {
			System.out.println("Starting 2...");
			getConfiguration();
			System.out.println("Wtf...?");
			String path = getConfiguration().getPath();
			// TODO: add a security handler that ties into the EAI platform permissions!
			System.out.println("Starting 3...");
			if (path == null) {
				path = "/";
			}
			else if (!path.startsWith("/")) {
				path = "/" + path;
			}
			ResourceContainer<?> publicDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PUBLIC);
			System.out.println("Path '" + path + "': " + publicDirectory);
			if (publicDirectory != null) {
				ResourceHandler handler = new ResourceHandler(publicDirectory, path, !EAIResourceRepository.isDevelopment());
				subscription = getConfiguration().getHttpServer().getServer().getEventDispatcher().subscribe(HTTPRequest.class, handler);
				subscription.filter(new PathFilter(path));
			}
		}
	}

}

package be.nabu.eai.repository.artifacts.http.virtual;

import java.io.IOException;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.resources.api.ResourceContainer;

public class VirtualHostArtifact extends JAXBArtifact<VirtualHostConfiguration> implements StartableArtifact, StoppableArtifact {

	private EventDispatcher dispatcher = new EventDispatcherImpl();
	private boolean started;
	
	public VirtualHostArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "virtual-host.xml", VirtualHostConfiguration.class);
	}

	public EventDispatcher getDispatcher() {
		return dispatcher;
	}

	@Override
	public void stop() throws IOException {
		if (started && getConfiguration().getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getConfiguration().getServer().getServer().unroute(getConfiguration().getHost());
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getConfiguration().getServer().getServer().unroute(host);
					}
				}
			}
			else {
				getConfiguration().getServer().getServer().unroute(null);
			}
		}
		started = false;
	}

	@Override
	public void start() throws IOException {
		if (getConfiguration().getServer() != null) {
			if (getConfiguration().getHost() != null) {
				getConfiguration().getServer().getServer().route(getConfiguration().getHost(), dispatcher);
				if (getConfiguration().getAliases() != null) {
					for (String host : getConfiguration().getAliases()) {
						getConfiguration().getServer().getServer().route(host, dispatcher);
					}
				}
			}
			else {
				getConfiguration().getServer().getServer().route(null, dispatcher);
			}
			started = true;
		}
	}

	@Override
	public boolean isStarted() {
		return started;
	}

}

package be.nabu.eai.repository.artifacts.web;

import java.io.IOException;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class WebFragmentArtifact extends JAXBArtifact<WebFragmentConfiguration> implements StartableArtifact, StoppableArtifact {

	public WebFragmentArtifact(String id, ResourceContainer<?> directory, Repository repository, String fileName, Class<WebFragmentConfiguration> configurationClazz) {
		super(id, directory, repository, "web-fragment.xml", WebFragmentConfiguration.class);
	}

	private boolean started;
	
	@Override
	public void stop() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws IOException {
		if (!started && getConfiguration().getWebArtifact() != null && getConfiguration().getWebArtifact().isStarted()) {
			
		}
	}

	@Override
	public boolean isStarted() {
		// TODO Auto-generated method stub
		return false;
	}

}

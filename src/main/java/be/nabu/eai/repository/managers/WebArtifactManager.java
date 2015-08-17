package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.html.WebArtifact;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.validator.api.Validation;

public class WebArtifactManager implements ArtifactManager<WebArtifact> {

	@Override
	public WebArtifact load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		return new WebArtifact(
			entry.getId(), 
			entry.getContainer() 
		);
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, WebArtifact artifact) throws IOException {
		artifact.save(entry.getContainer());
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return null;
	}

	@Override
	public Class<WebArtifact> getArtifactClass() {
		return WebArtifact.class;
	}

	@Override
	public List<String> getReferences(WebArtifact artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		if (artifact.getConfiguration().getHttpServer() != null) {
			references.add(artifact.getConfiguration().getHttpServer().getId());
		}
		return references;
	}

	@Override
	public List<Validation<?>> updateReference(WebArtifact artifact, String from, String to) throws IOException {
		if (artifact.getConfiguration().getHttpServer() != null) {
			if (from.equals(artifact.getConfiguration().getHttpServer().getId())) {
				artifact.getConfiguration().setHttpServer((DefinedHTTPServer) ArtifactResolverFactory.getInstance().getResolver().resolve(to));
			}
		}
		return null;
	}
}

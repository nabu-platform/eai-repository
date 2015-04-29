package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.libs.validator.api.ValidationMessage;

public class DefinedHTTPServerManager implements ArtifactManager<DefinedHTTPServer> {

	@Override
	public DefinedHTTPServer load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		return new DefinedHTTPServer(
			entry.getId(), 
			entry.getContainer() 
		);
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, DefinedHTTPServer artifact) throws IOException {
		artifact.save(entry.getContainer());
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return null;
	}

	@Override
	public Class<DefinedHTTPServer> getArtifactClass() {
		return DefinedHTTPServer.class;
	}

	@Override
	public List<String> getReferences(DefinedHTTPServer artifact) throws IOException {
		return null;
	}

	@Override
	public List<ValidationMessage> updateReference(DefinedHTTPServer artifact, String from, String to) throws IOException {
		return null;
	}
}

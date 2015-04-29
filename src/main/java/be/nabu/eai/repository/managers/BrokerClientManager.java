package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class BrokerClientManager implements ArtifactManager<DefinedBrokerClient> {

	@Override
	public DefinedBrokerClient load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		return new DefinedBrokerClient(
			entry.getId(), 
			entry.getContainer() 
		);
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, DefinedBrokerClient artifact) throws IOException {
		artifact.save(entry.getContainer());
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return null;
	}

	@Override
	public Class<DefinedBrokerClient> getArtifactClass() {
		return DefinedBrokerClient.class;
	}

	@Override
	public List<String> getReferences(DefinedBrokerClient artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		if (artifact.getConfiguration().getKeystore() != null) {
			references.add("artifact:/" + artifact.getConfiguration().getKeystore().getId());
		}
		if (artifact.getConfiguration().getEndpoint() != null) {
			references.add(artifact.getConfiguration().getEndpoint().toString());
		}
		return references;
	}

	@Override
	public List<ValidationMessage> updateReference(DefinedBrokerClient artifact, String from, String to) throws IOException {
		List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
		if (from.equals(artifact.getConfiguration().getKeystore().getId())) {
			artifact.getConfiguration().setKeystore((DefinedKeyStore) (to == null ? null : ArtifactResolverFactory.getInstance().getResolver().resolve(to)));
		}
		if (artifact.getConfiguration().getEndpoint() != null && from.equals(artifact.getConfiguration().getEndpoint().toString())) {
			try {
				artifact.getConfiguration().setEndpoint(new URI(URIUtils.encodeURI(to)));
			}
			catch (URISyntaxException e) {
				messages.add(new ValidationMessage(Severity.ERROR, "Not a valid uri: " + to));
			}
		}
		return messages;
	}

}

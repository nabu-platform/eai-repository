package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.libs.validator.api.ValidationMessage;

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
		return null;
	}

	@Override
	public Class<DefinedBrokerClient> getArtifactClass() {
		return DefinedBrokerClient.class;
	}

}

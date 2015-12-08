package be.nabu.eai.repository.managers;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.broker.BrokerConfiguration;
import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class BrokerClientManager extends JAXBArtifactManager<BrokerConfiguration, DefinedBrokerClient> {

	public BrokerClientManager() {
		super(DefinedBrokerClient.class);
	}

	@Override
	protected DefinedBrokerClient newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DefinedBrokerClient(id, container, repository);
	}

}

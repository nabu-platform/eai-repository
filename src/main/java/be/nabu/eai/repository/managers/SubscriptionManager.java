package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.eai.repository.artifacts.subscription.DefinedSubscription;
import be.nabu.eai.repository.artifacts.subscription.SubscriptionConfiguration;
import be.nabu.eai.repository.managers.util.JAXBArtifactManager;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.validator.api.ValidationMessage;

public class SubscriptionManager extends JAXBArtifactManager<SubscriptionConfiguration, DefinedSubscription> {

	public SubscriptionManager() {
		super(DefinedSubscription.class);
	}

	@Override
	protected DefinedSubscription newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DefinedSubscription(id, container, repository);
	}

	@Override
	public List<String> getReferences(DefinedSubscription artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		if (artifact.getConfiguration().getBrokerClient() != null) {
			references.add(artifact.getConfiguration().getBrokerClient().getId());
		}
		if (artifact.getConfiguration().getService() != null) {
			references.add(artifact.getConfiguration().getService().getId());
		}
		if (artifact.getConfiguration().getQueue() != null) {
			references.add(artifact.getConfiguration().getQueue().getId());
		}
		return references;
	}

	@Override
	public List<ValidationMessage> updateReference(DefinedSubscription artifact, String from, String to) throws IOException {
		if (artifact.getConfiguration().getBrokerClient() != null) {
			if (from.equals(artifact.getConfiguration().getBrokerClient().getId())) {
				artifact.getConfiguration().setBrokerClient((DefinedBrokerClient) ArtifactResolverFactory.getInstance().getResolver().resolve(to));
			}
		}
		if (artifact.getConfiguration().getService() != null) {
			if (from.equals(artifact.getConfiguration().getService().getId())) {
				artifact.getConfiguration().setService((DefinedService) ArtifactResolverFactory.getInstance().getResolver().resolve(to));
			}
		}
		if (artifact.getConfiguration().getQueue() != null) {
			if (from.equals(artifact.getConfiguration().getQueue().getId())) {
				artifact.getConfiguration().setQueue((DefinedType) ArtifactResolverFactory.getInstance().getResolver().resolve(to));
			}
		}
		return null;
	}

}

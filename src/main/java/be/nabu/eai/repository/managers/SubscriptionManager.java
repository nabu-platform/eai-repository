package be.nabu.eai.repository.managers;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.subscription.DefinedSubscription;
import be.nabu.eai.repository.artifacts.subscription.SubscriptionConfiguration;
import be.nabu.eai.repository.managers.util.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class SubscriptionManager extends JAXBArtifactManager<SubscriptionConfiguration, DefinedSubscription> {

	public SubscriptionManager() {
		super(DefinedSubscription.class);
	}

	@Override
	protected DefinedSubscription newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DefinedSubscription(id, container, repository);
	}

}

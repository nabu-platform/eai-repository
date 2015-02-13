package be.nabu.eai.repository.artifacts.subscription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.broker.client.SubscriptionException;
import be.nabu.eai.repository.InternalPrincipal;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.artifacts.subscription.SubscriptionConfiguration.Selector;
import be.nabu.libs.artifacts.api.RestartableArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.tasks.TaskUtils;
import be.nabu.libs.tasks.api.ExecutionException;
import be.nabu.libs.tasks.api.TaskExecutor;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;

public class DefinedSubscription extends JAXBArtifact<SubscriptionConfiguration> implements RestartableArtifact {

	private static final String AMOUNT_OF_SUBSCRIBERS = "be.nabu.eai.broker.amountOfSubscribers";
	private static final String PRIORITY = "be.nabu.eai.broker.priority";
	private static final String DELAY_INITIAL = "be.nabu.eai.broker.delayInitial";
	private static final String DELAY_DELTA = "be.nabu.eai.broker.delayDelta";
	private static final String DELAY_MAX = "be.nabu.eai.broker.delayMax";
	private Repository repository;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public DefinedSubscription(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, "subscription.xml", SubscriptionConfiguration.class);
		this.repository = repository;
	}
	
	@Override
	public void start() throws IOException {
		final SubscriptionConfiguration configuration = getConfiguration();
		if (repository.getServiceRunner() != null && notNull(configuration.getBrokerClient(), configuration.getService(), configuration.getSelector()) && !Boolean.TRUE.equals(configuration.getDisabled())) {
			try {
				String selector = configuration.getSelector().toString().toLowerCase();
				if (Selector.PARALLEL.equals(configuration.getSelector()) && configuration.getMaxParallel() != null && configuration.getMaxParallel() >= 0) {
					selector += ":" + configuration.getMaxParallel();
				}
				else if (Selector.GROUPED.equals(configuration.getSelector())) {
					if (configuration.getGroupQuery() == null || configuration.getGroupQuery().isEmpty()) {
						logger.error("Can not start subscription " + getId() + " because a grouped subscription requires a group query");
						return;
					}
					selector += ":" + configuration.getGroupQuery();
				}
				configuration.getBrokerClient().getBrokerClient().subscribe(
					getId(),
					configuration.getQueue().getId(),
					selector,
					configuration.getQuery(),
					TaskUtils.always(new TaskExecutor<ComplexContent>(){
						private Map<String, String> inputFields = new HashMap<String, String>();
						@Override
						public void execute(ComplexContent content) throws ExecutionException {
							// we need to find the input field that matches the type for this content
							// we cache the result so we don't have to inspect the service every time
							String typeId = ((DefinedType) content.getType()).getId();
							if (!inputFields.containsKey(typeId)) {
								synchronized(inputFields) {
									if (!inputFields.containsKey(typeId)) {
										for (Element<?> element : TypeUtils.getAllChildren(configuration.getService().getServiceInterface().getInputDefinition())) {
											if (element.getType() instanceof DefinedType && ((DefinedType) element.getType()).getId().equals(typeId)) {
												inputFields.put(typeId, element.getName());
												break;
											}
										}
									}
								}
							}
							if (!inputFields.containsKey(typeId)) {
								throw new RuntimeException("There is no matching input variable in the service " + configuration.getService().getId() + " for the type: " + typeId);
							}
							ComplexContent input = configuration.getService().getServiceInterface().getInputDefinition().newInstance();
							input.set(inputFields.get(typeId), content);
							// the interface allows both sync and async execution, the server is currently implemented as sync
							// but even if it is async, we need to send back information so this thread will hang until the server one is done
							Future<ServiceResult> result = repository.getServiceRunner().run(configuration.getService(), repository.newExecutionContext(new InternalPrincipal(configuration.getUserId(), getId())), input);
							try {
								ServiceResult serviceResult = result.get();
								if (serviceResult.getException() != null) {
									throw new ExecutionException(serviceResult.getException());
								}
								// TODO: currently we do nothing with the service result, do we want the ability to republish it or something?
							}
							catch (InterruptedException e) {
								throw new ExecutionException(e);
							}
							catch (java.util.concurrent.ExecutionException e) {
								throw new ExecutionException(e);
							}
						}
						@Override
						public Class<ComplexContent> getTaskClass() {
							return ComplexContent.class;
						}
					}), 
					configuration.getPriority() == null ? new Integer(System.getProperty(PRIORITY, "0")) : configuration.getPriority(), 
					configuration.getBestEffort() == null ? false : configuration.getBestEffort(), 
					configuration.getAmountOfSubscribers() == null ? new Integer(System.getProperty(AMOUNT_OF_SUBSCRIBERS, "1")) : configuration.getAmountOfSubscribers(), 
					configuration.getDedicated() == null ? false : configuration.getDedicated(),
					configuration.getDelayInitial() == null ? new Long(System.getProperty(DELAY_INITIAL, "1000")) : configuration.getDelayInitial(),
					configuration.getDelayDelta() == null ? new Long(System.getProperty(DELAY_DELTA, "1000")) : configuration.getDelayDelta(),
					configuration.getDelayMax() == null ? new Long(System.getProperty(DELAY_MAX, "60000")) : configuration.getDelayMax()
				);
			}
			catch (SubscriptionException e) {
				throw new IOException(e);
			}
		}
		else {
			logger.error("Can not start subscription " + getId() + " because the configuration is incomplete");
			stop();
		}
	}
	
	@Override
	public void stop() {
		try {
			if (getConfiguration().getBrokerClient() != null) {
				getConfiguration().getBrokerClient().getBrokerClient().unsubscribe(getId());
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void restart() throws IOException {
		start();
	}
}

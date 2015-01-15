package be.nabu.eai.repository.artifacts.subscription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import be.nabu.eai.broker.client.SubscriptionException;
import be.nabu.eai.repository.InternalPrincipal;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.artifacts.subscription.SubscriptionConfiguration.Selector;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.tasks.TaskUtils;
import be.nabu.libs.tasks.api.ExecutionException;
import be.nabu.libs.tasks.api.TaskExecutor;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;

public class DefinedSubscription extends JAXBArtifact<SubscriptionConfiguration> implements StartableArtifact, StoppableArtifact {

	private static final String AMOUNT_OF_SUBSCRIBERS = "be.nabu.eai.broker.amountOfSubscribers";
	private static final String PRIORITY = "be.nabu.eai.broker.priority";
	private static final String DELAY_INITIAL = "be.nabu.eai.broker.delayInitial";
	private static final String DELAY_DELTA = "be.nabu.eai.broker.delayDelta";
	private static final String DELAY_MAX = "be.nabu.eai.broker.delayMax";
	private Repository repository;
	
	public DefinedSubscription(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, "subscription.xml", SubscriptionConfiguration.class);
		this.repository = repository;
	}
		
	@Override
	public void start() throws IOException {
		final SubscriptionConfiguration configuration = getConfiguration();
		if (configuration.getBrokerClient() != null && configuration.getService() != null) {
			try {
				String selector = configuration.getSelector().toString().toLowerCase();
				if (Selector.PARALLEL.equals(configuration.getSelector()) && configuration.getMaxParallel() != null && configuration.getMaxParallel() >= 0) {
					selector += ":" + configuration.getMaxParallel();
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
							ServiceRuntime runtime = new ServiceRuntime(configuration.getService(), repository.newExecutionContext(new InternalPrincipal(configuration.getUserId(), getId())));
							ComplexContent input = configuration.getService().getServiceInterface().getInputDefinition().newInstance();
							input.set(inputFields.get(typeId), content);
							try {
								runtime.run(input);
							}
							catch (ServiceException e) {
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
	}

	@Override
	public void stop() {
		// TODO: remove remote subscription?
	}
}

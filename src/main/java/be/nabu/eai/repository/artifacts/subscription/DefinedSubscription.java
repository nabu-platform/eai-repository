package be.nabu.eai.repository.artifacts.subscription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.tasks.TaskUtils;
import be.nabu.libs.tasks.api.ExecutionException;
import be.nabu.libs.tasks.api.TaskExecutor;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class DefinedSubscription implements Artifact {

	private static final String AMOUNT_OF_SUBSCRIBERS = "be.nabu.eai.broker.amountOfSubscribers";
	private static final String PRIORITY = "be.nabu.eai.broker.priority";
	private ArtifactResolver<DefinedService> serviceResolver;
	private String id;
	private ResourceContainer<?> directory;
	private ArtifactResolver<DefinedBrokerClient> brokerClientResolver;
	
	private SubscriptionConfiguration configuration;

	public DefinedSubscription(String id, ResourceContainer<?> directory, ArtifactResolver<DefinedBrokerClient> brokerClientResolver, ArtifactResolver<DefinedService> serviceResolver) {
		this.id = id;
		this.directory = directory;
		this.brokerClientResolver = brokerClientResolver;
		this.serviceResolver = serviceResolver;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	public void save(ResourceContainer<?> directory) throws IOException {
		SubscriptionConfiguration configuration = getConfiguration();
		Resource target = directory.getChild("subscription.xml");
		if (target == null) {
			target = ((ManageableContainer<?>) directory).create("subscription.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
		try {
			configuration.marshal(IOUtils.toOutputStream(writable));
		}
		catch (JAXBException e) {
			throw new IOException(e);
		}
		finally {
			writable.close();
		}
	}
	
	public SubscriptionConfiguration getConfiguration() throws IOException {
		if (configuration == null) {
			synchronized(this) {
				if (configuration == null) {
					Resource target = directory.getChild("subscription.xml");
					if (target == null) {
						configuration = new SubscriptionConfiguration();
						configuration.setAmountOfSubscribers(new Integer(System.getProperty(AMOUNT_OF_SUBSCRIBERS, "1")));
						configuration.setBestEffort(false);
						configuration.setDedicated(false);
						configuration.setPriority(new Integer(System.getProperty(PRIORITY, "0")));
					}
					else {
						ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) target);
						try {
							configuration = SubscriptionConfiguration.unmarshal(IOUtils.toInputStream(readable));
						}
						catch (JAXBException e) {
							throw new IOException(e);
						}
						finally {
							readable.close();
						}
					}
				}
			}
		}
		return configuration;
	}
	
	public void start() throws IOException {
		if (getConfiguration().getClientId() != null && getConfiguration().getServiceId() != null) {
			final DefinedService service = serviceResolver.resolve(getConfiguration().getServiceId());
			DefinedBrokerClient brokerClient = brokerClientResolver.resolve(getConfiguration().getClientId());
			brokerClient.getBrokerClient().subscribe(
				getConfiguration().getSubscriptionId(), 
				TaskUtils.always(new TaskExecutor<ComplexContent>(){
					private Map<String, String> inputFields = new HashMap<String, String>();
					@Override
					public void execute(ComplexContent content) throws ExecutionException {
						// TODO
						// we need to find the input field that matches the type for this content
						// we cache the result so we don't have to inspect the service every time
						String typeId = ((DefinedType) content.getType()).getId();
						if (!inputFields.containsKey(typeId)) {
							synchronized(inputFields) {
								if (!inputFields.containsKey(typeId)) {
									// TODO
								}
							}
						}
					}
					@Override
					public Class<ComplexContent> getTaskClass() {
						return ComplexContent.class;
					}
				}), 
				getConfiguration().getPriority(), 
				getConfiguration().getBestEffort(), 
				getConfiguration().getAmountOfSubscribers(), 
				getConfiguration().getDedicated()
			);
		}
	}
}

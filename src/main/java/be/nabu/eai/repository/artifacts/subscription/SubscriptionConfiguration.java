package be.nabu.eai.repository.artifacts.subscription;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "subscription")
public class SubscriptionConfiguration {

	/**
	 * The subscription you are polling. This should match a subscription on the target server
	 */
	private String subscriptionId;
	/**
	 * The resulting service that should be called
	 */
	private String serviceId;
	/**
	 * The broker client that should be used
	 */
	private String clientId;
	
	private Integer priority, amountOfSubscribers;
	private Boolean bestEffort, dedicated;
	
	public String getSubscriptionId() {
		return subscriptionId;
	}
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	public Integer getAmountOfSubscribers() {
		return amountOfSubscribers;
	}
	public void setAmountOfSubscribers(Integer amountOfSubscribers) {
		this.amountOfSubscribers = amountOfSubscribers;
	}
	public Boolean getBestEffort() {
		return bestEffort;
	}
	public void setBestEffort(Boolean bestEffort) {
		this.bestEffort = bestEffort;
	}
	public Boolean getDedicated() {
		return dedicated;
	}
	public void setDedicated(Boolean dedicated) {
		this.dedicated = dedicated;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public static SubscriptionConfiguration unmarshal(InputStream input) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(SubscriptionConfiguration.class);
		return (SubscriptionConfiguration) context.createUnmarshaller().unmarshal(input);
	}
	
	public void marshal(OutputStream output) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(SubscriptionConfiguration.class);
		context.createMarshaller().marshal(this, output);
	}
}

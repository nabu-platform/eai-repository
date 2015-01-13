package be.nabu.eai.repository.artifacts.subscription;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "trigger")
@XmlType(propOrder = { "brokerClient", "subscriptionId", "service", "userId", "priority", "bestEffort", "amountOfSubscribers", "dedicated", "delayInitial", "delayDelta", "delayMax" })
public class SubscriptionConfiguration {
	
	private DefinedBrokerClient brokerClient;
	private DefinedService service;
	private Integer priority, amountOfSubscribers;
	private Long delayInitial, delayDelta, delayMax;
	private Boolean bestEffort, dedicated;
	private String userId;

	/**
	 * The subscription you are polling. This should match a subscription on the target server
	 */
	private String subscriptionId;
	
	@NotNull
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedBrokerClient getBrokerClient() {
		return brokerClient;
	}
	public void setBrokerClient(DefinedBrokerClient brokerClient) {
		this.brokerClient = brokerClient;
	}
	@NotNull
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getService() {
		return service;
	}
	public void setService(DefinedService service) {
		this.service = service;
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
	@NotNull
	public String getSubscriptionId() {
		return subscriptionId;
	}
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	public Long getDelayInitial() {
		return delayInitial;
	}
	public void setDelayInitial(Long delayInitial) {
		this.delayInitial = delayInitial;
	}
	public Long getDelayDelta() {
		return delayDelta;
	}
	public void setDelayDelta(Long delayDelta) {
		this.delayDelta = delayDelta;
	}
	public Long getDelayMax() {
		return delayMax;
	}
	public void setDelayMax(Long delayMax) {
		this.delayMax = delayMax;
	}
	@NotNull
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
}

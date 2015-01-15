package be.nabu.eai.repository.artifacts.subscription;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.artifacts.broker.DefinedBrokerClient;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.DefinedType;

@XmlRootElement(name = "trigger")
@XmlType(propOrder = { "brokerClient", "queue", "selector", "query", "service", "userId", "priority", "bestEffort", "amountOfSubscribers", "dedicated", "delayInitial", "delayDelta", "delayMax", "maxParallel" })
public class SubscriptionConfiguration {
	
	private DefinedBrokerClient brokerClient;
	private DefinedService service;
	private DefinedType queue;
	private String query;
	private Integer priority, amountOfSubscribers;
	private Long delayInitial, delayDelta, delayMax;
	private Boolean bestEffort, dedicated;
	private String userId;
	private Selector selector;
	private Integer maxParallel;
	
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
	@NotNull
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedType getQueue() {
		return queue;
	}
	public void setQueue(DefinedType queue) {
		this.queue = queue;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	@NotNull
	public Selector getSelector() {
		return selector;
	}
	public void setSelector(Selector selector) {
		this.selector = selector;
	}
	public Integer getMaxParallel() {
		return maxParallel;
	}
	public void setMaxParallel(Integer maxParallel) {
		this.maxParallel = maxParallel;
	}


	public enum Selector {
		PARALLEL,
		SERIAL
	}
}

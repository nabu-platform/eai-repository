package be.nabu.eai.repository.artifacts.broker;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "broker")
public class BrokerConfiguration {
	
	private String clientId, username, password, keystoreId;
	private URI endpoint;
	private String encoding;
	private int processingPoolSize, connectionPoolSize, connectionTimeout, socketTimeout;
	private long pollInterval;
	
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getKeystoreId() {
		return keystoreId;
	}
	public void setKeystoreId(String keystoreId) {
		this.keystoreId = keystoreId;
	}
	public URI getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(URI endpoint) {
		this.endpoint = endpoint;
	}
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	public int getProcessingPoolSize() {
		return processingPoolSize;
	}
	public void setProcessingPoolSize(int processingPoolSize) {
		this.processingPoolSize = processingPoolSize;
	}
	public int getConnectionPoolSize() {
		return connectionPoolSize;
	}
	public void setConnectionPoolSize(int connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	public int getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	public long getPollInterval() {
		return pollInterval;
	}
	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}
	
	public static BrokerConfiguration unmarshal(InputStream input) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(BrokerConfiguration.class);
		return (BrokerConfiguration) context.createUnmarshaller().unmarshal(input);
	}
	
	public void marshal(OutputStream output) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(BrokerConfiguration.class);
		context.createMarshaller().marshal(this, output);
	}
}

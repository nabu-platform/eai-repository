package be.nabu.eai.repository.artifacts.broker;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "broker")
@XmlType(propOrder = { "endpoint", "keystore", "username", "password", "clientId", "encoding", "processingPoolSize", "connectionPoolSize", "connectionTimeout", "socketTimeout", "pollInterval" })
public class BrokerConfiguration {
	
	private DefinedKeyStore keystore;
	private String clientId, username, password;
	private URI endpoint;
	private String encoding;
	private Integer processingPoolSize, connectionPoolSize, connectionTimeout, socketTimeout;
	private Long pollInterval;
	
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
	public Integer getProcessingPoolSize() {
		return processingPoolSize;
	}
	public void setProcessingPoolSize(Integer processingPoolSize) {
		this.processingPoolSize = processingPoolSize;
	}
	public Integer getConnectionPoolSize() {
		return connectionPoolSize;
	}
	public void setConnectionPoolSize(Integer connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	public Long getPollInterval() {
		return pollInterval;
	}
	public void setPollInterval(Long pollInterval) {
		this.pollInterval = pollInterval;
	}
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedKeyStore getKeystore() {
		return keystore;
	}
	public void setKeystore(DefinedKeyStore keystore) {
		this.keystore = keystore;
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

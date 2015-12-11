package be.nabu.eai.repository.artifacts.broker;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.security.EncryptionXmlAdapter;

@XmlRootElement(name = "broker")
@XmlType(propOrder = { "endpoint", "keystore", "username", "password", "clientId", "encoding", "processingPoolSize", "connectionPoolSize", "connectionTimeout", "socketTimeout", "deviation" })
public class BrokerConfiguration {
	
	private DefinedKeyStore keystore;
	private String clientId, username, password;
	private URI endpoint;
	private String encoding;
	private Integer processingPoolSize, connectionPoolSize, connectionTimeout, socketTimeout;
	private Double deviation;
	
	@EnvironmentSpecific
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	@EnvironmentSpecific
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value=EncryptionXmlAdapter.class)
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@EnvironmentSpecific
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
	
	@EnvironmentSpecific
	public Integer getProcessingPoolSize() {
		return processingPoolSize;
	}
	public void setProcessingPoolSize(Integer processingPoolSize) {
		this.processingPoolSize = processingPoolSize;
	}
	
	@EnvironmentSpecific
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
	public Double getDeviation() {
		return deviation;
	}
	public void setDeviation(Double deviation) {
		this.deviation = deviation;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedKeyStore getKeystore() {
		return keystore;
	}
	public void setKeystore(DefinedKeyStore keystore) {
		this.keystore = keystore;
	}
}

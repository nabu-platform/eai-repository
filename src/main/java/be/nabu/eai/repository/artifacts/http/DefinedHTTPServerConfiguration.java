package be.nabu.eai.repository.artifacts.http;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.io.SSLServerMode;

@XmlRootElement(name = "httpServer")
@XmlType(propOrder = { "keystore", "sslServerMode", "port", "poolSize", "socketTimeout", "ioPoolSize", "maxTotalConnections", "maxConnectionsPerClient", "maxSizePerRequest" })
public class DefinedHTTPServerConfiguration {
	private Integer port;
	private DefinedKeyStore keystore;
	private SSLServerMode sslServerMode;
	private Integer poolSize, socketTimeout, ioPoolSize, maxTotalConnections, maxConnectionsPerClient;
	private Long maxSizePerRequest;
	
	@EnvironmentSpecific
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedKeyStore getKeystore() {
		return keystore;
	}
	public void setKeystore(DefinedKeyStore keystore) {
		this.keystore = keystore;
	}
	
	@EnvironmentSpecific
	public Integer getPoolSize() {
		return poolSize;
	}
	public void setPoolSize(Integer poolSize) {
		this.poolSize = poolSize;
	}
	
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	
	@EnvironmentSpecific
	public SSLServerMode getSslServerMode() {
		return sslServerMode;
	}
	public void setSslServerMode(SSLServerMode sslServerMode) {
		this.sslServerMode = sslServerMode;
	}
	
	@EnvironmentSpecific
	public Integer getIoPoolSize() {
		return ioPoolSize;
	}
	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}
	
	@EnvironmentSpecific
	public Integer getMaxTotalConnections() {
		return maxTotalConnections;
	}
	public void setMaxTotalConnections(Integer maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}
	
	@EnvironmentSpecific
	public Integer getMaxConnectionsPerClient() {
		return maxConnectionsPerClient;
	}
	public void setMaxConnectionsPerClient(Integer maxConnectionsPerClient) {
		this.maxConnectionsPerClient = maxConnectionsPerClient;
	}
	
	public Long getMaxSizePerRequest() {
		return maxSizePerRequest;
	}
	public void setMaxSizePerRequest(Long maxSizePerRequest) {
		this.maxSizePerRequest = maxSizePerRequest;
	}
	
}

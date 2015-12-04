package be.nabu.eai.repository.artifacts.http;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.io.SSLServerMode;

@XmlRootElement(name = "httpServer")
@XmlType(propOrder = { "keystore", "sslServerMode", "port", "poolSize", "socketTimeout", "ioPoolSize" })
public class DefinedHTTPServerConfiguration {
	private Integer port;
	private DefinedKeyStore keystore;
	private SSLServerMode sslServerMode;
	private Integer poolSize, socketTimeout, ioPoolSize;

	public Integer getPort() {
		return port;
	}
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedKeyStore getKeystore() {
		return keystore;
	}
	public Integer getPoolSize() {
		return poolSize;
	}
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public void setKeystore(DefinedKeyStore keystore) {
		this.keystore = keystore;
	}
	public void setPoolSize(Integer poolSize) {
		this.poolSize = poolSize;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	public SSLServerMode getSslServerMode() {
		return sslServerMode;
	}
	public void setSslServerMode(SSLServerMode sslServerMode) {
		this.sslServerMode = sslServerMode;
	}
	public Integer getIoPoolSize() {
		return ioPoolSize;
	}
	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}
}

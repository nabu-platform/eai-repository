package be.nabu.eai.repository.artifacts.proxy;

import java.net.Proxy.Type;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.utils.security.EncryptionXmlAdapter;

@XmlRootElement(name = "proxy")
@XmlType(propOrder = { "host", "port", "domain", "username", "password", "type", "bypass" })
public class ProxyConfiguration {
	private String host, username, password, domain, bypass;
	private int port;
	private Type type;
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	@XmlJavaTypeAdapter(value=EncryptionXmlAdapter.class)
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public String getBypass() {
		return bypass;
	}
	public void setBypass(String bypass) {
		this.bypass = bypass;
	}
}

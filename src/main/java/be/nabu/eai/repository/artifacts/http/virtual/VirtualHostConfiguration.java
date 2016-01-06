package be.nabu.eai.repository.artifacts.http.virtual;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "virtualHost")
@XmlType(propOrder = { "host", "aliases", "server", "keyAlias" })
public class VirtualHostConfiguration {
	private String host;
	private String keyAlias;
	private List<String> aliases;
	private DefinedHTTPServer server;
	
	@EnvironmentSpecific
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@EnvironmentSpecific
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	
	@EnvironmentSpecific
	public String getKeyAlias() {
		return keyAlias;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedHTTPServer getServer() {
		return server;
	}
	public void setServer(DefinedHTTPServer server) {
		this.server = server;
	}
	
}

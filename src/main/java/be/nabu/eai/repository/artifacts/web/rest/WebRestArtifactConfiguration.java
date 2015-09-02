package be.nabu.eai.repository.artifacts.web.rest;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.artifacts.web.WebArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.types.api.DefinedType;

@XmlRootElement(name = "webRestArtifact")
public class WebRestArtifactConfiguration {

	private DefinedType input, output;
	private WebArtifact webArtifact;
	private String path, queryParameters, cookieParameters, sessionParameters, headerParameters;
	private String role;

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public WebArtifact getWebArtifact() {
		return webArtifact;
	}
	public void setWebArtifact(WebArtifact webArtifact) {
		this.webArtifact = webArtifact;
	}

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getQueryParameters() {
		return queryParameters;
	}
	public void setQueryParameters(String queryParameters) {
		this.queryParameters = queryParameters;
	}
	public String getCookieParameters() {
		return cookieParameters;
	}
	public void setCookieParameters(String cookieParameters) {
		this.cookieParameters = cookieParameters;
	}
	public String getSessionParameters() {
		return sessionParameters;
	}
	public void setSessionParameters(String sessionParameters) {
		this.sessionParameters = sessionParameters;
	}
	public String getHeaderParameters() {
		return headerParameters;
	}
	public void setHeaderParameters(String headerParameters) {
		this.headerParameters = headerParameters;
	}
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedType getInput() {
		return input;
	}
	public void setInput(DefinedType input) {
		this.input = input;
	}
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedType getOutput() {
		return output;
	}
	public void setOutput(DefinedType output) {
		this.output = output;
	}
}

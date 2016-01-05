package be.nabu.eai.repository.artifacts.web;

import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

public class WebFragmentConfiguration {
	private String path;
	private String whitelistedCodes;
	private List<DefinedService> restServices;
	private WebArtifact webArtifact;
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getWhitelistedCodes() {
		return whitelistedCodes;
	}
	public void setWhitelistedCodes(String whitelistedCodes) {
		this.whitelistedCodes = whitelistedCodes;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedService> getRestServices() {
		return restServices;
	}
	public void setRestServices(List<DefinedService> restServices) {
		this.restServices = restServices;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public WebArtifact getWebArtifact() {
		return webArtifact;
	}
	public void setWebArtifact(WebArtifact webArtifact) {
		this.webArtifact = webArtifact;
	}
}

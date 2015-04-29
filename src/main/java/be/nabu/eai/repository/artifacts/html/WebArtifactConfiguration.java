package be.nabu.eai.repository.artifacts.html;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "webArtifact")
public class WebArtifactConfiguration {
	private DefinedHTTPServer httpServer;
	private String path;

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedHTTPServer getHttpServer() {
		return httpServer;
	}

	public void setHttpServer(DefinedHTTPServer httpServer) {
		this.httpServer = httpServer;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}

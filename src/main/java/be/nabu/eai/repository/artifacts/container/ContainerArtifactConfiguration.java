package be.nabu.eai.repository.artifacts.container;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.util.ClassAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;

@XmlRootElement(name = "container")
public class ContainerArtifactConfiguration {
	
	private Class<?> artifactManagerClass;
	private Map<String, String> configuration;
	
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getConfiguration() {
		return configuration;
	}
	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}
	
	@XmlJavaTypeAdapter(ClassAdapter.class)
	public Class<?> getArtifactManagerClass() {
		return artifactManagerClass;
	}
	public void setArtifactManagerClass(Class<?> artifactManagerClass) {
		this.artifactManagerClass = artifactManagerClass;
	}
}

package be.nabu.eai.repository.artifacts.simpleType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.ValueEnumerator;
import be.nabu.eai.repository.util.KeyValueMapAdapter;

@XmlRootElement(name = "simpleType")
public class SimpleTypeConfiguration {
	private String parent;
	private List<String> enumerations;
	private Map<String, String> properties;
	
	@ValueEnumerator(enumerator = SimpleTypeEnumerator.class)
	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}
	
	public List<String> getEnumerations() {
		return enumerations;
	}
	public void setEnumerations(List<String> enumerations) {
		this.enumerations = enumerations;
	}
	
	@XmlJavaTypeAdapter(KeyValueMapAdapter.class)
	public Map<String, String> getProperties() {
		// always has to have a value because it is then passed by reference to the maincontroller and the updates to it can be seen
		if (properties == null) {
			properties = new LinkedHashMap<String, String>();
		}
		return properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}

package be.nabu.eai.repository.util;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class KeyValuePair {
	private String key, value;

	public KeyValuePair(String key, String value) {
		this.key = key;
		this.value = value;
	}
	public KeyValuePair() {
		// auto construction
	}

	@XmlAttribute
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@XmlValue
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}

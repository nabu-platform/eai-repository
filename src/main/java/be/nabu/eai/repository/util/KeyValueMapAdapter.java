package be.nabu.eai.repository.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class KeyValueMapAdapter extends XmlAdapter<KeyValueMapAdapter.MapRoot, Map<String, String>> {

	public static class MapRoot {
		private List<KeyValuePair> properties = new ArrayList<KeyValuePair>();

		@XmlElement(name = "property")
		public List<KeyValuePair> getProperties() {
			return properties;
		}
		public void setProperties(List<KeyValuePair> properties) {
			this.properties = properties;
		}
	}
	
	@Override
	public Map<String, String> unmarshal(MapRoot v) throws Exception {
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (v == null) {
			return map;
		}
		for (KeyValuePair pair : v.getProperties()) {
			map.put(pair.getKey(), pair.getValue() == null || pair.getValue().trim().isEmpty() ? null : pair.getValue());
		}
		return map;
	}

	@Override
	public MapRoot marshal(Map<String, String> v) throws Exception {
		if (v == null) {
			return null;
		}
		MapRoot root = new MapRoot();
		for (String key : v.keySet()) {
			root.getProperties().add(new KeyValuePair(key, v.get(key)));
		}
		return root;
	}

}

package be.nabu.eai.repository.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class KeyValueMapAdapter extends XmlAdapter<ArrayList<KeyValuePair>, Map<String, String>> {

	@Override
	public Map<String, String> unmarshal(ArrayList<KeyValuePair> v) throws Exception {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (KeyValuePair pair : v) {
			map.put(pair.getKey(), pair.getValue());
		}
		return map;
	}

	@Override
	public ArrayList<KeyValuePair> marshal(Map<String, String> v) throws Exception {
		ArrayList<KeyValuePair> list = new ArrayList<KeyValuePair>();
		for (String key : v.keySet()) {
			list.add(new KeyValuePair(key, v.get(key)));
		}
		return list;
	}

}

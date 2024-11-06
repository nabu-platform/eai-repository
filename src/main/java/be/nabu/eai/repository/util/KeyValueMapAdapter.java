/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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

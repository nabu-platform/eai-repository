package be.nabu.eai.repository.util;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import be.nabu.eai.repository.util.KeyValueMapAdapter.MapRoot;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.impl.Base64Decoder;
import be.nabu.utils.io.IOUtils;

public class KeyBytesMapAdapter extends XmlAdapter<KeyValueMapAdapter.MapRoot, Map<String, byte[]>> {
	
	@Override
	public Map<String, byte[]> unmarshal(MapRoot v) throws Exception {
		Map<String, byte[]> map = new LinkedHashMap<String, byte[]>();
		if (v == null) {
			return map;
		}
		for (KeyValuePair pair : v.getProperties()) {
			map.put(pair.getKey(), pair.getValue() == null || pair.getValue().trim().isEmpty() ? null : 
				IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(pair.getValue().getBytes(), true), new Base64Decoder())));
		}
		return map;
	}

	@Override
	public MapRoot marshal(Map<String, byte[]> v) throws Exception {
		if (v == null) {
			return null;
		}
		MapRoot root = new MapRoot();
		for (String key : v.keySet()) {
			root.getProperties().add(new KeyValuePair(key, 
				new String(IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(v.get(key), true), new Base64Decoder())))));
		}
		return root;
	}

}

package be.nabu.eai.repository.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ClassAdapter extends XmlAdapter<String, Class<?>> {

	@Override
	public Class<?> unmarshal(String v) throws Exception {
		return v == null ? null : Thread.currentThread().getContextClassLoader().loadClass(v);
	}

	@Override
	public String marshal(Class<?> v) throws Exception {
		return v == null ? null : v.getName();
	}
}

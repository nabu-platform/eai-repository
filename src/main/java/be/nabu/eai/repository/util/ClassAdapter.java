package be.nabu.eai.repository.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import be.nabu.eai.repository.EAIResourceRepository;

public class ClassAdapter extends XmlAdapter<String, Class<?>> {

	@Override
	public Class<?> unmarshal(String v) throws Exception {
		if (v == null) {
			return null;
		}
		Class<?> clazz = EAIResourceRepository.getInstance().loadClass(v);
		if (clazz == null) {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(v);
		}
		return clazz;
	}

	@Override
	public String marshal(Class<?> v) throws Exception {
		return v == null ? null : v.getName();
	}
}

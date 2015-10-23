package be.nabu.eai.repository.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

@SuppressWarnings("rawtypes")
public class ClassXMLAdapter extends XmlAdapter<String, Class> {

	@Override
	public Class unmarshal(String v) throws Exception {
		return v == null ? null : Thread.currentThread().getContextClassLoader().loadClass(v);
	}

	@Override
	public String marshal(Class v) throws Exception {
		return v == null ? null : v.getName();
	}

}

package be.nabu.eai.repository.jaxb;

import java.nio.charset.Charset;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class CharsetAdapter extends XmlAdapter<String, Charset> {

	@Override
	public Charset unmarshal(String v) throws Exception {
		return v == null ? null : Charset.forName(v);
	}

	@Override
	public String marshal(Charset v) throws Exception {
		return v == null ? null : v.name();
	}

}

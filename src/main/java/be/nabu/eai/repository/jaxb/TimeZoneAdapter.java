package be.nabu.eai.repository.jaxb;

import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class TimeZoneAdapter extends XmlAdapter<String, TimeZone> {

	@Override
	public TimeZone unmarshal(String v) throws Exception {
		return v == null ? null : TimeZone.getTimeZone(v);
	}

	@Override
	public String marshal(TimeZone v) throws Exception {
		return v == null ? null : v.getID();
	}

}

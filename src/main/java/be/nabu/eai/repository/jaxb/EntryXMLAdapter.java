package be.nabu.eai.repository.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Repository;

public class EntryXMLAdapter extends XmlAdapter<String, Entry> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private Repository repository;;
	
	public EntryXMLAdapter(Repository repository) {
		this.repository = repository;
	}
	
	public EntryXMLAdapter() {
		this(null);
	}
	
	@Override
	public Entry unmarshal(String v) throws Exception {
		try {
			Repository repository = this.repository == null ? EAIResourceRepository.getInstance() : this.repository;
			return v == null ? null : repository.getEntry(v);
		}
		catch (Exception e) {
			logger.error("Could not load artifact: " + v, e);
			return null;
		}
	}

	@Override
	public String marshal(Entry v) throws Exception {
		return v == null ? null : v.getId();
	}

}

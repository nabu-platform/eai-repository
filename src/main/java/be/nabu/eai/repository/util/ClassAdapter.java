package be.nabu.eai.repository.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;

public class ClassAdapter extends XmlAdapter<String, Class<?>> {

	private Repository repository;

	public ClassAdapter() {
		this(EAIResourceRepository.getInstance());
	}
	
	public ClassAdapter(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public Class<?> unmarshal(String v) throws Exception {
		if (v == null) {
			return null;
		}
		return repository.getClassLoader().loadClass(v);
	}

	@Override
	public String marshal(Class<?> v) throws Exception {
		return v == null ? null : v.getName();
	}
}

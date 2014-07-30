package be.nabu.eai.repository.managers.util;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.wsdl.WSDLWrapper;

public class WSDLClient implements Artifact {

	private String id;
	private WSDLWrapper wrapper;
	
	public WSDLClient(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	public WSDLWrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(WSDLWrapper wrapper) {
		this.wrapper = wrapper;
	}
}

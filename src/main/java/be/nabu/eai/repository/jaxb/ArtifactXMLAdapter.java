package be.nabu.eai.repository.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;

public class ArtifactXMLAdapter extends XmlAdapter<String, Artifact> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public Artifact unmarshal(String v) throws Exception {
		try {
			return v == null ? null : ArtifactResolverFactory.getInstance().getResolver().resolve(v);
		}
		catch (Exception e) {
			logger.error("Could not load artifact: " + v, e);
			return null;
		}
	}

	@Override
	public String marshal(Artifact v) throws Exception {
		return v == null ? null : v.getId();
	}

}

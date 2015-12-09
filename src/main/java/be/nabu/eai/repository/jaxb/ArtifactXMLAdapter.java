package be.nabu.eai.repository.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactResolver;

public class ArtifactXMLAdapter extends XmlAdapter<String, Artifact> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ArtifactResolver<?> resolver;
	
	public ArtifactXMLAdapter(ArtifactResolver<?> resolver) {
		this.resolver = resolver;
	}
	
	public ArtifactXMLAdapter() {
		this(ArtifactResolverFactory.getInstance().getResolver());
	}
	
	@Override
	public Artifact unmarshal(String v) throws Exception {
		try {
			return v == null ? null : resolver.resolve(v);
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

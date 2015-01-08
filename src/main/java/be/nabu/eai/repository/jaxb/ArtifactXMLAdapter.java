package be.nabu.eai.repository.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;

public class ArtifactXMLAdapter extends XmlAdapter<String, Artifact> {

	@Override
	public Artifact unmarshal(String v) throws Exception {
		return v == null ? null : ArtifactResolverFactory.getInstance().getResolver().resolve(v);
	}

	@Override
	public String marshal(Artifact v) throws Exception {
		return v == null ? null : v.getId();
	}

}

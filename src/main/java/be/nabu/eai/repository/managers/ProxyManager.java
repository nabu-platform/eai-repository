package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.proxy.DefinedProxy;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class ProxyManager implements ArtifactManager<DefinedProxy> {

	@Override
	public DefinedProxy load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		return new DefinedProxy(entry.getId(), entry.getContainer());
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, DefinedProxy artifact) throws IOException {
		try {
			artifact.save(entry.getContainer());
		}
		catch (IOException e) {
			return Arrays.asList(new ValidationMessage(Severity.ERROR, "Could not save proxy: " + e.getMessage()));
		}
		return null;
	}

	@Override
	public Class<DefinedProxy> getArtifactClass() {
		return DefinedProxy.class;
	}

}

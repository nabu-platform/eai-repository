package be.nabu.eai.repository.managers;

import java.io.IOException;
import java.security.KeyStoreException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class KeyStoreManager implements ArtifactManager<DefinedKeyStore> {

	@Override
	public DefinedKeyStore load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		return new DefinedKeyStore(entry.getId(), entry.getContainer());
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, DefinedKeyStore artifact) throws IOException {
		try {
			artifact.save(entry.getContainer());
		}
		catch (KeyStoreException e) {
			return Arrays.asList(new ValidationMessage(Severity.ERROR, "Could not save keystore: " + e.getMessage()));
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return null;
	}

	@Override
	public Class<DefinedKeyStore> getArtifactClass() {
		return DefinedKeyStore.class;
	}

	@Override
	public List<String> getReferences(DefinedKeyStore artifact) throws IOException {
		return null;
	}

	@Override
	public List<ValidationMessage> updateReference(DefinedKeyStore artifact, String from, String to) throws IOException {
		return null;
	}
}

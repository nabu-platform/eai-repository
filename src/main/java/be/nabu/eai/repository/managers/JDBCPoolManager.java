package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.jdbc.JDBCPool;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * TODO: don't start this when on developer machine
 * Should only start the pools when on the server
 * System wide setting or constructor setting or something?
 */
public class JDBCPoolManager implements ArtifactManager<JDBCPool> {
	
	@Override
	public JDBCPool load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Resource resource = entry.getContainer().getChild("jdbcpool.properties");
		if (resource == null) {
			throw new FileNotFoundException("Can not find jdbcpool.properties");
		}
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			JDBCPool pool = new JDBCPool(entry.getId());
			Properties properties = new Properties();
			properties.load(IOUtils.toInputStream(readable));
			pool.setConfig(properties);
			return pool;
		}
		finally {
			readable.close();
		}
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, JDBCPool artifact) throws IOException {
		Resource resource = entry.getContainer().getChild("jdbcpool.properties");
		if (resource == null) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create("jdbcpool.properties", "text/plain");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) resource);
		try {
			artifact.getConfig().store(IOUtils.toOutputStream(writable), null);
		}
		finally {
			writable.close();
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return new ArrayList<Validation<?>>();
	}

	@Override
	public List<String> getReferences(JDBCPool artifact) {
		List<String> references = new ArrayList<String>();
		// the reference is to an external system: the database
		Object object = artifact.getConfig().get("jdbcUrl");
		if (object != null) {
			references.add(object.toString());
		}
		return references;
	}

	@Override
	public Class<JDBCPool> getArtifactClass() {
		return JDBCPool.class;
	}

	@Override
	public List<Validation<?>> updateReference(JDBCPool artifact, String from, String to) {
		if (from.equals(artifact.getConfig().get("jdbcUrl"))) {
			artifact.getConfig().put("jdbcUrl", to);
		}
		return null;
	}
}

package be.nabu.eai.repository.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.jdbc.JDBCService;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class JDBCServiceManager implements ArtifactManager<JDBCService>, ArtifactRepositoryManager<JDBCService> {

	@Override
	public JDBCService load(ResourceEntry entry, List<ValidationMessage> messages) throws IOException, ParseException {
		JDBCService service = new JDBCService(entry.getId());
		Resource resource = entry.getContainer().getChild("jdbcservice.xml");
		if (resource == null) {
			throw new FileNotFoundException("Can not find jdbcpool.properties");
		}
		XMLBinding binding = new XMLBinding(new BeanType<JDBCServiceConfig>(JDBCServiceConfig.class), Charset.forName("UTF-8"));
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			JDBCServiceConfig config = TypeUtils.getAsBean(binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]), JDBCServiceConfig.class);
			service.setSql(config.getSql());
			service.setConnectionId(config.getConnectionId());
		}
		finally {
			readable.close();
		}
		service.setParameters((DefinedStructure) StructureManager.parse(entry, "parameters.xml"));
		service.setResults((DefinedStructure) StructureManager.parse(entry, "results.xml"));
		return service;
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, JDBCService artifact) throws IOException {
		JDBCServiceConfig config = new JDBCServiceConfig();
		config.setConnectionId(artifact.getConnectionId());
		config.setSql(artifact.getSql());
		StructureManager.format(entry, artifact.getParameters(), "parameters.xml");
		StructureManager.format(entry, artifact.getResults(), "results.xml");
		Resource resource = entry.getContainer().getChild("jdbcservice.xml");
		if (resource == null) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create("jdbcservice.xml", "application/xml");
		}
		XMLBinding binding = new XMLBinding(new BeanType<JDBCServiceConfig>(JDBCServiceConfig.class), Charset.forName("UTF-8"));
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) resource);
		try {
			binding.marshal(IOUtils.toOutputStream(writable), new BeanInstance<JDBCServiceConfig>(config));
		}
		finally {
			writable.close();
		}
		
		return new ArrayList<ValidationMessage>();
	}

	@Override
	public Class<JDBCService> getArtifactClass() {
		return JDBCService.class;
	}
	
	@XmlRootElement(name = "jdbcService")
	public static class JDBCServiceConfig {
		private String connectionId, sql;

		public String getConnectionId() {
			return connectionId;
		}

		public void setConnectionId(String connectionId) {
			this.connectionId = connectionId;
		}

		public String getSql() {
			return sql;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}
	}

	@Override
	public void addChildren(ModifiableEntry parent, JDBCService artifact) throws IOException {
		((EAINode) parent.getNode()).setLeaf(false);
		
		EAINode node = new EAINode();
		node.setArtifactClass(DefinedType.class);
		node.setArtifact(artifact.getParameters());
		node.setLeaf(true);
		Entry parameters = new MemoryEntry(parent.getRepository(), parent, node, parent.getId() + "." + JDBCService.PARAMETERS, JDBCService.PARAMETERS);
		// need to explicitly set id (it was loaded from file)
		artifact.getParameters().setId(parameters.getId());
		node.setEntry(parameters);
		parent.addChildren(parameters);
		
		node = new EAINode();
		node.setArtifactClass(DefinedType.class);
		node.setArtifact(artifact.getResults());
		node.setLeaf(true);
		Entry results = new MemoryEntry(parent.getRepository(), parent, node, parent.getId() + "." + JDBCService.RESULTS, JDBCService.RESULTS);
		artifact.getResults().setId(results.getId());
		node.setEntry(results);
		parent.addChildren(results);
	}
}

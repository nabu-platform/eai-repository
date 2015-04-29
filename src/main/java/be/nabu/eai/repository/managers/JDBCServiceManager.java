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
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.Node;
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
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
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
		XMLBinding binding = new XMLBinding((ComplexType) BeanResolver.getInstance().resolve(JDBCServiceConfig.class), Charset.forName("UTF-8"));
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			JDBCServiceConfig config = TypeUtils.getAsBean(binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]), JDBCServiceConfig.class);
			service.setSql(config.getSql());
			service.setConnectionId(config.getConnectionId());
			if (config.getInputDefinition() != null) {
				Node node = entry.getRepository().getNode(config.getInputDefinition());
				if (node == null) {
					throw new IllegalArgumentException("Could not find referenced input node: " + config.getInputDefinition());
				}
				service.setParameters((ComplexType) node.getArtifact());
				service.setInputGenerated(false);
			}
			if (config.getOutputDefinition() != null) {
				Node node = entry.getRepository().getNode(config.getOutputDefinition());
				if (node == null) {
					throw new IllegalArgumentException("Could not find referenced output node: " + config.getOutputDefinition());
				}
				service.setResults((ComplexType) node.getArtifact());
				service.setOutputGenerated(false);
			}
		}
		finally {
			readable.close();
		}
		if (service.isInputGenerated()) {
			service.setParameters((DefinedStructure) StructureManager.parse(entry, "parameters.xml"));
		}
		if (service.isOutputGenerated()) {
			service.setResults((DefinedStructure) StructureManager.parse(entry, "results.xml"));
		}
		return service;
	}

	@Override
	public List<ValidationMessage> save(ResourceEntry entry, JDBCService artifact) throws IOException {
		JDBCServiceConfig config = new JDBCServiceConfig();
		config.setConnectionId(artifact.getConnectionId());
		config.setSql(artifact.getSql());
		if (artifact.isInputGenerated()) {
			StructureManager.format(entry, artifact.getParameters(), "parameters.xml");
		}
		else {
			config.setInputDefinition(((DefinedType) artifact.getParameters()).getId());
			((ManageableContainer<?>) entry.getContainer()).delete("parameters.xml");
		}
		if (artifact.isOutputGenerated()) {
			StructureManager.format(entry, artifact.getResults(), "results.xml");
		}
		else {
			config.setOutputDefinition(((DefinedType) artifact.getResults()).getId());
			((ManageableContainer<?>) entry.getContainer()).delete("results.xml");
		}
		Resource resource = entry.getContainer().getChild("jdbcservice.xml");
		if (resource == null) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create("jdbcservice.xml", "application/xml");
		}
		XMLBinding binding = new XMLBinding((ComplexType) BeanResolver.getInstance().resolve(JDBCServiceConfig.class), Charset.forName("UTF-8"));
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) resource);
		try {
			binding.marshal(IOUtils.toOutputStream(writable), new BeanInstance<JDBCServiceConfig>(config));
		}
		finally {
			writable.close();
		}
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getReferences(artifact));
		}
		return new ArrayList<ValidationMessage>();
	}

	@Override
	public Class<JDBCService> getArtifactClass() {
		return JDBCService.class;
	}
	
	@XmlRootElement(name = "jdbcService")
	public static class JDBCServiceConfig {
		private String connectionId, sql, inputDefinition, outputDefinition;

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

		public String getInputDefinition() {
			return inputDefinition;
		}

		public void setInputDefinition(String inputDefinition) {
			this.inputDefinition = inputDefinition;
		}

		public String getOutputDefinition() {
			return outputDefinition;
		}

		public void setOutputDefinition(String outputDefinition) {
			this.outputDefinition = outputDefinition;
		}
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry parent, JDBCService artifact) {
		List<Entry> entries = new ArrayList<Entry>();
		if (artifact.isInputGenerated() || artifact.isOutputGenerated()) {
			boolean isLeaf = true;
			if (artifact.isInputGenerated() && TypeUtils.getAllChildren(artifact.getParameters()).size() > 0) {
				isLeaf = false;
				EAINode node = new EAINode();
				node.setArtifactClass(DefinedType.class);
				node.setArtifact((DefinedType) artifact.getParameters());
				node.setLeaf(true);
				Entry parameters = new MemoryEntry(parent.getRepository(), parent, node, parent.getId() + "." + JDBCService.PARAMETERS, JDBCService.PARAMETERS);
				// need to explicitly set id (it was loaded from file)
				((DefinedStructure) artifact.getParameters()).setId(parameters.getId());
				node.setEntry(parameters);
				parent.addChildren(parameters);
				entries.add(parameters);
			}
			if (artifact.isOutputGenerated() && TypeUtils.getAllChildren(artifact.getResults()).size() > 0) {
				isLeaf = false;
				EAINode node = new EAINode();
				node.setArtifactClass(DefinedType.class);
				node.setArtifact((DefinedType) artifact.getResults());
				node.setLeaf(true);
				Entry results = new MemoryEntry(parent.getRepository(), parent, node, parent.getId() + "." + JDBCService.RESULTS, JDBCService.RESULTS);
				((DefinedStructure) artifact.getResults()).setId(results.getId());
				node.setEntry(results);
				parent.addChildren(results);
				entries.add(results);
			}
			((EAINode) parent.getNode()).setLeaf(isLeaf);
		}
		return entries;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, JDBCService artifact) {
		List<Entry> entries = new ArrayList<Entry>();
		Entry parameters = parent.getChild(JDBCService.PARAMETERS);
		if (parameters != null) {
			((ModifiableEntry) parent).removeChildren(parameters.getName());
			entries.add(parameters);
		}
		Entry results = parent.getChild(JDBCService.RESULTS);
		if (results != null) {
			((ModifiableEntry) parent).removeChildren(results.getName());
			entries.add(results);
		}
		((EAINode) parent.getNode()).setLeaf(true);
		return entries;
	}
	
	
	public void refreshChildren(ModifiableEntry parent, JDBCService artifact) {
		removeChildren((ModifiableEntry) parent, artifact);
		addChildren((ModifiableEntry) parent, artifact);
	}

	@Override
	public List<String> getReferences(JDBCService artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		if (artifact.getConnectionId() != null) {
			references.add(artifact.getConnectionId());
		}
		references.addAll(StructureManager.getComplexReferences(artifact.getParameters()));
		references.addAll(StructureManager.getComplexReferences(artifact.getResults()));
		return references;
	}

	@Override
	public List<ValidationMessage> updateReference(JDBCService artifact, String from, String to) throws IOException {
		if (from.equals(artifact.getConnectionId())) {
			artifact.setConnectionId(to);
		}
		List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
		messages.addAll(StructureManager.updateReferences(artifact.getParameters(), from, to));
		messages.addAll(StructureManager.updateReferences(artifact.getResults(), from, to));
		return messages;
	}
}

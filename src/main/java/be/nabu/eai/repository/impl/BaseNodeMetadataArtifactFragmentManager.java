package be.nabu.eai.repository.impl;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactFragmentManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public abstract class BaseNodeMetadataArtifactFragmentManager<T extends Artifact> implements ArtifactFragmentManager<T> {

	private static final String METADATA_PATH = "metadata.xml";
	private static final String NODE_PATH = "node.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	protected List<ArtifactFragment> getSharedFragments(T artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>();
		fragments.add(new MetadataFragment(artifact));
		return fragments;
	}

	@Override
	public String getArtifactType(T artifact) {
		return artifact.getClass().getSimpleName();
	}

	@Override
	public List<Validation<?>> updateFragment(T artifact, String path, String oldContent, String newContent) {
		if (!METADATA_PATH.equals(path)) {
			throw new UnsupportedOperationException("Updating fragments is only supported for metadata.xml");
		}
		Entry entry = EAIResourceRepository.getInstance().getEntry(artifact.getId());
		if (!(entry instanceof RepositoryEntry)) {
			throw new UnsupportedOperationException("Updating metadata.xml is only supported for repository entries with node.xml");
		}
		RepositoryEntry repositoryEntry = (RepositoryEntry) entry;
		Resource nodeResource = repositoryEntry.getContainer().getChild(NODE_PATH);
		if (nodeResource == null) {
			throw new UnsupportedOperationException("Updating metadata.xml requires an existing node.xml");
		}
		List<Validation<?>> validations = new ArrayList<Validation<?>>();
		Document document = parseMetadata(newContent, validations);
		if (document == null || hasErrors(validations)) {
			return validations;
		}
		EAINode node = repositoryEntry.getNode();
		applyMetadata(document.getDocumentElement(), node, validations);
		if (!hasErrors(validations)) {
			repositoryEntry.saveNode();
		}
		return validations;
	}

	private void applyMetadata(Element metadata, EAINode node, List<Validation<?>> validations) {
		node.setName(readValue(metadata, "title"));
		node.setSummary(readValue(metadata, "summary"));
		node.setDescription(readValue(metadata, "description"));
		node.setComment(readValue(metadata, "comment"));
		node.setReference(readValue(metadata, "reference"));
		node.setDeprecated(parseDate(readValue(metadata, "deprecated"), "deprecated", validations));
		node.setTags(readTags(metadata, validations));
	}

	private Document parseMetadata(String content, List<Validation<?>> validations) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
			Element root = document.getDocumentElement();
			if (root == null || !"metadata".equals(root.getLocalName() == null ? root.getNodeName() : root.getLocalName())) {
				validations.add(new ValidationMessage(Severity.ERROR, "The metadata fragment must contain a metadata root element"));
				return null;
			}
			return document;
		}
		catch (Exception e) {
			validations.add(new ValidationMessage(Severity.ERROR, "Invalid metadata.xml: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage())));
			return null;
		}
	}

	private List<String> readTags(Element metadata, List<Validation<?>> validations) {
		List<String> tags = new ArrayList<String>();
		Element tagsElement = getFirstChild(metadata, "tags");
		if (tagsElement == null) {
			return null;
		}
		NodeList children = tagsElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			org.w3c.dom.Node child = children.item(i);
			if (child instanceof Element) {
				Element element = (Element) child;
				String name = element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
				if (!"tag".equals(name)) {
					validations.add(new ValidationMessage(Severity.ERROR, "Unsupported metadata tags child: " + name));
				}
				else {
					tags.add(normalize(element.getTextContent()));
				}
			}
		}
		return tags;
	}

	private Element getFirstChild(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			org.w3c.dom.Node child = children.item(i);
			if (child instanceof Element) {
				Element element = (Element) child;
				String childName = element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
				if (name.equals(childName)) {
					return element;
				}
			}
		}
		return null;
	}

	private String readValue(Element metadata, String name) {
		Element child = getFirstChild(metadata, name);
		return child == null ? null : normalize(child.getTextContent());
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private Boolean parseBoolean(String value, String field, List<Validation<?>> validations) {
		if (value == null) {
			return null;
		}
		if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
			return Boolean.parseBoolean(value);
		}
		validations.add(new ValidationMessage(Severity.ERROR, "Invalid boolean value for " + field + ": " + value));
		return null;
	}

	private Integer parseInteger(String value, String field, List<Validation<?>> validations) {
		if (value == null) {
			return null;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			validations.add(new ValidationMessage(Severity.ERROR, "Invalid integer value for " + field + ": " + value));
			return null;
		}
	}

	private Date parseDate(String value, String field, List<Validation<?>> validations) {
		if (value == null) {
			return null;
		}
		try {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
			formatter.setLenient(false);
			return formatter.parse(value);
		}
		catch (ParseException e) {
			validations.add(new ValidationMessage(Severity.ERROR, "Invalid date value for " + field + ": " + value + ". Expected format: " + DATE_FORMAT));
			return null;
		}
	}

	private boolean hasErrors(List<Validation<?>> validations) {
		if (validations == null) {
			return false;
		}
		for (Validation<?> validation : validations) {
			if (validation != null && validation.getSeverity() == Severity.ERROR) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getGuidelines(List<String> fragmentTypes) {
		if (fragmentTypes == null || fragmentTypes.isEmpty() || fragmentTypes.contains("metadata")) {
			return "## Fragment: metadata\n\n"
				+ "Use `metadata.xml` to update repository node metadata that surrounds the artifact itself.\n\n"
				+ "Supported fields include:\n"
				+ "- `title`\n"
				+ "- `summary`\n"
				+ "- `description`\n"
				+ "- `comment`\n"
				+ "- `reference`\n"
				+ "- `deprecated`\n"
				+ "- `tags/tag`\n\n"
				+ "Notes:\n"
				+ "- This updates `node.xml`, not the artifact content file.\n"
				+ "- Unknown or invalid values return validation errors.\n"
				+ "- `deprecated` must use `yyyy-MM-dd'T'HH:mm:ss`.\n\n"
				+ "Example:\n"
				+ "```xml\n"
				+ "<metadata>\n"
				+ "\t<title>Customer API</title>\n"
				+ "\t<summary>Public customer operations</summary>\n"
				+ "\t<description>Used by storefront and CRM flows.</description>\n"
				+ "\t<comment>Owned by integration team</comment>\n"
				+ "\t<reference>DOC-123</reference>\n"
				+ "\t<deprecated></deprecated>\n"
				+ "\t<tags>\n"
				+ "\t\t<tag>customer</tag>\n"
				+ "\t\t<tag>public</tag>\n"
				+ "\t</tags>\n"
				+ "</metadata>\n"
				+ "```";
		}
		return null;
	}

	private class MetadataFragment implements ArtifactFragment {

		private T artifact;
		private Entry entry;

		public MetadataFragment(T artifact) {
			this.artifact = artifact;
			this.entry = EAIResourceRepository.getInstance().getEntry(artifact.getId());
		}

		@Override
		public boolean isEditable() {
			return entry instanceof ResourceEntry;
		}

		@Override
		public boolean isRemovable() {
			return false;
		}

		@Override
		public String getPath() {
			return METADATA_PATH;
		}

		@Override
		public String getContent() {
			Node node = EAIResourceRepository.getInstance().getNode(artifact.getId());
			try {
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element metadata = document.createElement("metadata");
				document.appendChild(metadata);
				append(document, metadata, "artifactId", artifact.getId());
				append(document, metadata, "title", node == null ? null : node.getName());
				append(document, metadata, "summary", node == null ? null : node.getSummary());
				append(document, metadata, "description", node == null ? null : node.getDescription());
				append(document, metadata, "comment", node == null ? null : node.getComment());
				append(document, metadata, "reference", node == null ? null : node.getReference());
				append(document, metadata, "deprecated", node != null && node.getDeprecated() != null ? node.getDeprecated().toString() : null);
				Element tags = document.createElement("tags");
				metadata.appendChild(tags);
				if (node != null && node.getTags() != null) {
					for (String tag : node.getTags()) {
						append(document, tags, "tag", tag);
					}
				}
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				StringWriter writer = new StringWriter();
				transformer.transform(new DOMSource(document), new StreamResult(writer));
				return writer.toString();
			}
			catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
			catch (TransformerException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String getContentType() {
			return CONTENT_TYPE;
		}

		@Override
		public String getArtifactId() {
			return artifact.getId();
		}

		@Override
		public String getFragmentType() {
			return "metadata";
		}

		@Override
		public Map<String, String> getProperties() {
			return Collections.emptyMap();
		}
	}

	private void append(Document document, Element parent, String name, String value) {
		Element element = document.createElement(name);
		if (value != null) {
			element.setTextContent(value);
		}
		parent.appendChild(element);
	}
}

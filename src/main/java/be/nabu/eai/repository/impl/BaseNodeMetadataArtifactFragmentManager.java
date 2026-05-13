package be.nabu.eai.repository.impl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
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

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactFragmentManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.api.Artifact;

public abstract class BaseNodeMetadataArtifactFragmentManager<T extends Artifact> implements ArtifactFragmentManager<T> {

	private static final String METADATA_PATH = "metadata.xml";
	private static final String CONTENT_TYPE = "application/xml";
	
	protected List<ArtifactFragment> getSharedFragments(T artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>();
		fragments.add(new MetadataFragment(artifact));
		return fragments;
	}

	protected String getArtifactType(T artifact) {
		return artifact.getClass().getSimpleName();
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
				append(document, metadata, "hidden", node != null ? Boolean.toString(node.isHidden()) : null);
				append(document, metadata, "locked", node != null ? Boolean.toString(node.isLocked()) : null);
				append(document, metadata, "priority", node != null && node.getPriority() != null ? node.getPriority().toString() : null);
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
		public String getArtifactType() {
			return BaseNodeMetadataArtifactFragmentManager.this.getArtifactType(artifact);
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

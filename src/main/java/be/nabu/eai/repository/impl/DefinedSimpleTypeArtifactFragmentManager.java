package be.nabu.eai.repository.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validation;

public class DefinedSimpleTypeArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<DefinedSimpleType> {

	private static final String SIMPLE_TYPE_PATH = "scalar.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String EXTENSION_HIERARCHY = "extension-hierarchy";
	private static final String ARTIFACT_TYPE = "scalar";
	private static final String ARTIFACT_CATEGORY = "type";

	@Override
	public List<ArtifactFragment> listFragments(DefinedSimpleType artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>(getSharedFragments(artifact));
		fragments.add(new ArtifactFragment() {
			@Override
			public boolean isEditable() {
				return false;
			}

			@Override
			public boolean isRemovable() {
				return false;
			}

			@Override
			public String getPath() {
				return SIMPLE_TYPE_PATH;
			}

			@Override
			public String getContent() {
				return serialize(artifact);
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
				return "scalar";
			}

			@Override
			public Map<String, String> getProperties() {
				Map<String, String> properties = new LinkedHashMap<String, String>();
				String extensionHierarchy = getExtensionHierarchy(artifact);
				if (!extensionHierarchy.isEmpty()) {
					properties.put(EXTENSION_HIERARCHY, extensionHierarchy);
				}
				return properties;
			}

			@Override
			public Long getLastModified() {
				return getFragmentLastModified(artifact.getId(), SIMPLE_TYPE_PATH);
			}
		});
		return fragments;
	}

	private String serialize(DefinedSimpleType artifact) {
		StringBuilder builder = new StringBuilder();
		builder.append("<scalar");
		appendAttribute(builder, "id", artifact.getId());
		appendAttribute(builder, "name", artifact.getName());
		appendAttribute(builder, "namespace", artifact.getNamespace());
		Type superType = artifact.getSuperType();
		if (superType instanceof DefinedType) {
			appendAttribute(builder, "superType", ((DefinedType) superType).getId());
		}
		else if (superType != null) {
			appendAttribute(builder, "superType", superType.getName());
		}
		appendAttribute(builder, "instanceClass", artifact.getInstanceClass() == null ? null : artifact.getInstanceClass().getName());
		builder.append(">\n");
		for (Value<?> value : artifact.getProperties()) {
			if (value == null || value.getProperty() == null) {
				continue;
			}
			builder.append("\t<property");
			appendAttribute(builder, "name", value.getProperty().getName());
			appendAttribute(builder, "value", value.getValue() == null ? null : stringify(value.getValue()));
			builder.append("/>\n");
		}
		builder.append("</scalar>");
		return builder.toString();
	}

	private String stringify(Object value) {
		if (value instanceof List<?>) {
			List<String> values = new ArrayList<String>();
			for (Object child : (List<?>) value) {
				values.add(child == null ? "" : child.toString());
			}
			return String.join(",", values);
		}
		return value.toString();
	}

	private void appendAttribute(StringBuilder builder, String name, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		builder.append(" ").append(name).append("=\"").append(escape(value)).append("\"");
	}

	private String escape(String value) {
		return value
			.replace("&", "&amp;")
			.replace("\"", "&quot;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private String getExtensionHierarchy(DefinedSimpleType artifact) {
		List<String> hierarchy = new ArrayList<String>();
		Type parent = artifact.getSuperType();
		while (parent != null) {
			if (parent instanceof DefinedType) {
				String id = ((DefinedType) parent).getId();
				if (id != null && !id.isEmpty()) {
					hierarchy.add(id);
				}
			}
			parent = parent.getSuperType();
		}
		return String.join(",", hierarchy);
	}

	@Override
	public List<Validation<?>> updateFragment(DefinedSimpleType artifact, String path, String oldContent, String newContent) {
		throw new UnsupportedOperationException("Updating fragments is not supported for simple types");
	}

	@Override
	public List<Validation<?>> deleteFragment(DefinedSimpleType artifact, String path) {
		throw new UnsupportedOperationException("Deleting fragments is not supported for simple types");
	}

	@Override
	public List<Validation<?>> createFragment(DefinedSimpleType artifact, String path, String content) {
		throw new UnsupportedOperationException("Creating fragments is not supported for simple types");
	}

	@Override
	public String getGuidelines(List<String> fragmentTypes) {
		List<String> filtered = new ArrayList<String>();
		if (fragmentTypes == null || fragmentTypes.isEmpty() || fragmentTypes.contains("scalar") || fragmentTypes.contains("scalar.xml")) {
			filtered.add("# Artifact: scalar\n\nFragments:\n- `metadata.xml`: repository metadata around the artifact\n- `scalar.xml`: read-only scalar summary\n");
			filtered.add(super.getGuidelines(Arrays.asList("metadata")));
			filtered.add("## Fragment: scalar.xml\n\n"
				+ "Notes:\n"
				+ "- `property` entries contain flattened values; list values are joined with commas.\n\n"
				+ "Example:\n"
				+ "```xml\n"
				+ "<scalar id=\"example.types.customerCode\" name=\"customerCode\" superType=\"java.lang.String\" instanceClass=\"java.lang.String\">\n"
				+ "\t<property name=\"pattern\" value=\"[A-Z0-9]+\"/>\n"
				+ "\t<property name=\"minLength\" value=\"1\"/>\n"
				+ "</scalar>\n"
				+ "```");
		}
		return filtered.isEmpty() ? null : String.join("\n\n", filtered);
	}

	@Override
	public Class<DefinedSimpleType> getArtifactClass() {
		return DefinedSimpleType.class;
	}

	@Override
	public String getArtifactType() {
		return ARTIFACT_TYPE;
	}

	@Override
	public String getArtifactCategory() {
		return ARTIFACT_CATEGORY;
	}

}

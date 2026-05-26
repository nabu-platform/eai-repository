package be.nabu.eai.repository.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.validator.api.Validation;

public class DefinedTypeArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<DefinedType> {

	private static final String STRUCTURE_PATH = "structure.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String EXTENSION_HIERARCHY = "extension-hierarchy";
	private static final String ARTIFACT_CATEGORY = "type";
	private static final String COMPLEX_ARTIFACT_TYPE = "structure";
	private static final String GUIDELINES_PATH = "/guidelines/structure.md";

	@Override
	public List<ArtifactFragment> listFragments(DefinedType artifact) {
		if (!(artifact instanceof ComplexType)) {
			return getSharedFragments(artifact);
		}
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
				return STRUCTURE_PATH;
			}

			@Override
			public String getContent() {
				XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
				marshaller.setIgnoreUnknownSuperTypes(true);
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try {
					marshaller.marshal(output, (ComplexType) artifact);
					return new String(output.toByteArray(), "UTF-8");
				}
				catch (IOException e) {
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
				return "structure";
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
				return getFragmentLastModified(artifact.getId(), STRUCTURE_PATH);
			}
		});
		return fragments;
	}

	private String getExtensionHierarchy(DefinedType artifact) {
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
	public List<Validation<?>> updateFragment(DefinedType artifact, String path, String oldContent, String newContent) {
		throw new UnsupportedOperationException("Updating fragments is not supported for defined types");
	}

	@Override
	public List<Validation<?>> deleteFragment(DefinedType artifact, String path) {
		throw new UnsupportedOperationException("Deleting fragments is not supported for defined types");
	}

	@Override
	public List<Validation<?>> createFragment(DefinedType artifact, String path, String content) {
		throw new UnsupportedOperationException("Creating fragments is not supported for defined types");
	}

	@Override
	public String getGuidelines(List<String> fragmentTypes) {
		List<String> filtered = new ArrayList<String>();
		if (fragmentTypes == null || fragmentTypes.isEmpty() || fragmentTypes.contains("structure") || fragmentTypes.contains("structure.xml")) {
			filtered.add(loadGuidelinesResource(GUIDELINES_PATH));
			filtered.add(buildSimpleTypeGuidelines());
			filtered.add(super.getGuidelines(Arrays.asList("metadata")));
		}
		return filtered.isEmpty() ? null : String.join("\n\n", filtered);
	}

	private String loadGuidelinesResource(String resourcePath) {
		return EAIRepositoryUtils.loadCachedClasspathResource(DefinedTypeArtifactFragmentManager.class, resourcePath);
	}

	private String buildSimpleTypeGuidelines() {
		List<String> lines = new ArrayList<String>();
		appendRepositorySimpleTypes(lines);
		return lines.isEmpty() ? null : String.join("\n", lines);
	}

	private void appendRepositorySimpleTypes(List<String> lines) {
		EAIResourceRepository repository = EAIResourceRepository.getInstance();
		if (repository == null) {
			return;
		}
		List<DefinedSimpleType> simpleTypes = repository.getArtifacts(DefinedSimpleType.class);
		if (simpleTypes == null || simpleTypes.isEmpty()) {
			return;
		}
		List<String> entries = new ArrayList<String>();
		for (DefinedSimpleType simpleType : simpleTypes) {
			if (simpleType == null || simpleType.getId() == null || simpleType.getId().trim().isEmpty()) {
				continue;
			}
			entries.add("- `" + simpleType.getId() + "` (name: `" + simpleType.getName() + "`, instance: `" + (simpleType.getInstanceClass() == null ? "unknown" : simpleType.getInstanceClass().getName()) + "`, superType: `" + describeType(simpleType.getSuperType()) + "`, properties: " + describeProperties(simpleType) + ")");
		}
		if (entries.isEmpty()) {
			return;
		}
		Collections.sort(entries);
		lines.add("");
		lines.add("Repository-defined simple types currently available:");
		lines.addAll(entries);
	}

	private String describeType(Type type) {
		if (type == null) {
			return "none";
		}
		if (type instanceof DefinedType) {
			String id = ((DefinedType) type).getId();
			if (id != null && !id.trim().isEmpty()) {
				return id;
			}
		}
		String name = type.getName();
		return name == null || name.trim().isEmpty() ? type.getClass().getName() : name;
	}

	private String describeProperties(DefinedSimpleType simpleType) {
		List<String> properties = new ArrayList<String>();
		for (Value<?> value : simpleType.getProperties()) {
			if (value == null || value.getProperty() == null || value.getProperty().getName() == null) {
				continue;
			}
			StringBuilder builder = new StringBuilder();
			builder.append(value.getProperty().getName());
			if (value.getValue() != null) {
				builder.append("=").append(String.valueOf(value.getValue()));
			}
			properties.add(builder.toString());
		}
		return properties.isEmpty() ? "none" : String.join(", ", properties);
	}

	@Override
	public Class<DefinedType> getArtifactClass() {
		return DefinedType.class;
	}
	
	@Override
	public String getArtifactType() {
		return COMPLEX_ARTIFACT_TYPE;
	}

	@Override
	public String getArtifactCategory() {
		return ARTIFACT_CATEGORY;
	}

}

package be.nabu.eai.repository.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.validator.api.Validation;

public class DefinedTypeArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<DefinedType> {

	private static final String STRUCTURE_PATH = "structure.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String EXTENSION_HIERARCHY = "extension-hierarchy";
	private static final String COMPLEX_ARTIFACT_TYPE = "complexType";
	private static final String SIMPLE_ARTIFACT_TYPE = "simpleType";

	@Override
	public List<ArtifactFragment> listFragments(DefinedType artifact) {
		if (!(artifact instanceof ComplexType)) {
			throw new UnsupportedOperationException("Fragment support is currently only available for complex defined types");
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
		if (fragmentTypes == null || fragmentTypes.isEmpty() || fragmentTypes.contains("structure")) {
			return "Defined complex types expose a read-only structure.xml fragment with the XML type definition. Simple defined types are not supported.";
		}
		return null;
	}

	@Override
	public Class<DefinedType> getArtifactClass() {
		return DefinedType.class;
	}
	
	@Override
	public String getArtifactType(DefinedType artifact) {
		return artifact instanceof ComplexType ? COMPLEX_ARTIFACT_TYPE : SIMPLE_ARTIFACT_TYPE;
	}

}

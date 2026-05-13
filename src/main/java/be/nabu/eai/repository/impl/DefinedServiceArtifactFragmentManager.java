package be.nabu.eai.repository.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.validator.api.Validation;

public class DefinedServiceArtifactFragmentManager<T extends DefinedService> extends BaseNodeMetadataArtifactFragmentManager<T> {

	private static final String INPUT_PATH = "input.xml";
	private static final String OUTPUT_PATH = "output.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String ARTIFACT_TYPE = "service";

	@Override
	public List<ArtifactFragment> listFragments(T artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>(getSharedFragments(artifact));
		fragments.addAll(Arrays.<ArtifactFragment>asList(
			new ServiceFragment(artifact, INPUT_PATH, artifact.getServiceInterface().getInputDefinition()),
			new ServiceFragment(artifact, OUTPUT_PATH, artifact.getServiceInterface().getOutputDefinition())
		));
		return fragments;
	}

	@Override
	public List<Validation<?>> updateFragment(T artifact, String path, String oldContent, String newContent) {
		throw new UnsupportedOperationException("Updating fragments is not supported for defined services");
	}

	@Override
	public List<Validation<?>> deleteFragment(T artifact, String path) {
		throw new UnsupportedOperationException("Deleting fragments is not supported for defined services");
	}

	@Override
	public List<Validation<?>> createFragment(T artifact, String path, String content) {
		throw new UnsupportedOperationException("Creating fragments is not supported for defined services");
	}

	@Override
	public String getGuidelines() {
		return "Defined services expose read-only input and output fragments with the XML type definitions of the service interface.";
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<T> getArtifactClass() {
		return (Class<T>) DefinedService.class;
	}

	private class ServiceFragment implements ArtifactFragment {

		private T artifact;
		private String path;
		private ComplexType type;

		public ServiceFragment(T artifact, String path, ComplexType type) {
			this.artifact = artifact;
			this.path = path;
			this.type = type;
		}

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
			return path;
		}

		@Override
		public String getContent() {
			XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
			marshaller.setIgnoreUnknownSuperTypes(true);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try {
				marshaller.marshal(output, type);
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
		public String getArtifactType() {
			return DefinedServiceArtifactFragmentManager.this.getArtifactType(artifact);
		}

		@Override
		public Map<String, String> getProperties() {
			return Collections.emptyMap();
		}
	}

	@Override
	protected String getArtifactType(T artifact) {
		return ARTIFACT_TYPE;
	}
	
}

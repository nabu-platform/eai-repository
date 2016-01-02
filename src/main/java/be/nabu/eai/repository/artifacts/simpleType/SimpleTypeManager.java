package be.nabu.eai.repository.artifacts.simpleType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.validator.api.Validation;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SimpleTypeManager extends JAXBArtifactManager<SimpleTypeConfiguration, SimpleTypeArtifact<Object>> {

	public SimpleTypeManager() {
		super(getTheClass());
	}

	@Override
	protected SimpleTypeArtifact<Object> newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new SimpleTypeArtifact(id, container, repository);
	}
	
	private static Class getTheClass() {
		return SimpleTypeArtifact.class; 
	}

	@Override
	public List<String> getReferences(SimpleTypeArtifact<Object> artifact) throws IOException {
		List<String> references = new ArrayList<String>(super.getReferences(artifact));
		if (artifact.getConfiguration().getParent() != null) {
			references.add(artifact.getConfiguration().getParent());
		}
		return references;
	}

	@Override
	public List<Validation<?>> updateReference(SimpleTypeArtifact<Object> artifact, String from, String to) throws IOException {
		if (from.equals(artifact.getConfiguration().getParent())) {
			artifact.getConfiguration().setParent(to);
		}
		return super.updateReference(artifact, from, to);
	}
	
	
}

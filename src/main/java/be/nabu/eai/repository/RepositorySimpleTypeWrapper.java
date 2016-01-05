package be.nabu.eai.repository;

import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.SimpleTypeWrapper;

public class RepositorySimpleTypeWrapper implements SimpleTypeWrapper {

	private Repository repository;

	public RepositorySimpleTypeWrapper(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public <T> DefinedSimpleType<T> wrap(Class<T> object) {
		return null;
	}

	@Override
	public DefinedSimpleType<?> getByName(String name) {
		if (name.startsWith("[")) {
			return null;
		}
		Node node = repository.getNode(name);
		if (node != null && DefinedSimpleType.class.isAssignableFrom(node.getArtifactClass())) {
			try {
				return (DefinedSimpleType<?>) node.getArtifact();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

}

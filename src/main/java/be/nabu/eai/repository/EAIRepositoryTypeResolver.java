package be.nabu.eai.repository;

import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeResolver;

public class EAIRepositoryTypeResolver implements DefinedTypeResolver {

	private EAIResourceRepository repository;
	
	public EAIRepositoryTypeResolver(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedType resolve(String id) {
		return (DefinedType) repository.resolve(id);
	}
}

package be.nabu.eai.repository;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeResolver;

public class EAIRepositoryTypeResolver implements DefinedTypeResolver {

	private EAIResourceRepository repository;
	
	public EAIRepositoryTypeResolver(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedType resolve(String id) {
		String part = null;
		int index = id.indexOf(':');
		if (index > 0) {
			part = id.substring(index + 1);
			id = id.substring(0, index);
		}
		Artifact artifact = repository.resolve(id);
		if (artifact instanceof DefinedType) {
			return (DefinedType) artifact;
		}
		else if (artifact instanceof DefinedService) {
			if ("input".equals(part)) {
				return new DynamicallyDefinedComplexType(id + ":" + part, ((DefinedService) artifact).getServiceInterface().getInputDefinition());
			}
			else if ("output".equals(part)) {
				return new DynamicallyDefinedComplexType(id + ":" + part, ((DefinedService) artifact).getServiceInterface().getOutputDefinition());
			}
		}
		return null;
	}
}

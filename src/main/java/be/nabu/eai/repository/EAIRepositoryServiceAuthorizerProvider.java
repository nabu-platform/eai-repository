package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.services.MultipleServiceAuthorizer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ServiceAuthorizer;
import be.nabu.libs.services.api.ServiceAuthorizerProvider;

public class EAIRepositoryServiceAuthorizerProvider implements ServiceAuthorizerProvider {

	private EAIResourceRepository repository;

	public EAIRepositoryServiceAuthorizerProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public ServiceAuthorizer getAuthorizer(ServiceRuntime runtime) {
		List<ServiceAuthorizer> authorizers = new ArrayList<ServiceAuthorizer>();
		for (ServiceAuthorizerProvider authorizerProvider : repository.getArtifactsThatImplement(ServiceAuthorizerProvider.class)) {
			ServiceAuthorizer authorizer = authorizerProvider.getAuthorizer(runtime);
			if (authorizer != null) {
				authorizers.add(authorizer);
			}
		}
		return new MultipleServiceAuthorizer(authorizers, true);
	}

	public EAIResourceRepository getRepository() {
		return repository;
	}

}

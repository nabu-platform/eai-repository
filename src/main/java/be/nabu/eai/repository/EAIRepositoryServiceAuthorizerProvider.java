package be.nabu.eai.repository;

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
		// TODO Auto-generated method stub
		return null;
	}

	public EAIResourceRepository getRepository() {
		return repository;
	}

}

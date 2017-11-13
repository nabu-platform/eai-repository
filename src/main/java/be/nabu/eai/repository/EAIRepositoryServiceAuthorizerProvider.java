package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.services.MultipleServiceAuthorizer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceAuthorizer;
import be.nabu.libs.services.api.ServiceAuthorizerProvider;
import be.nabu.libs.types.api.ComplexContent;

public class EAIRepositoryServiceAuthorizerProvider implements ServiceAuthorizerProvider {

	private EAIResourceRepository repository;

	public EAIRepositoryServiceAuthorizerProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public ServiceAuthorizer getAuthorizer(ServiceRuntime runtime) {
		// if it is running as root, don't check permissions, he can do anything
		if (SystemPrincipal.ROOT.equals(runtime.getExecutionContext().getSecurityContext().getToken())) {
			return null;
		}
		else if (runtime.getExecutionContext().getSecurityContext().getToken() instanceof SystemPrincipal) {
			return new ServiceAuthorizer() {
				@Override
				public boolean canRun(ServiceRuntime runtime, ComplexContent input) {
					// system principals must have the permission of the system to run something
					if (runtime.getService() instanceof DefinedService && runtime.getExecutionContext().getSecurityContext().getToken() instanceof SystemPrincipal
							&& runtime.getExecutionContext().getSecurityContext().getPermissionHandler() != null && 
							!runtime.getExecutionContext().getSecurityContext().getPermissionHandler().hasPermission(runtime.getExecutionContext().getSecurityContext().getToken(), ((DefinedService) runtime.getService()).getId(), "execute")) {
						return false;
					}
					return true;
				}
			};
		}
		else {
			List<ServiceAuthorizer> authorizers = new ArrayList<ServiceAuthorizer>();
			for (ServiceAuthorizerProvider authorizerProvider : repository.getArtifacts(ServiceAuthorizerProvider.class)) {
				ServiceAuthorizer authorizer = authorizerProvider.getAuthorizer(runtime);
				if (authorizer != null) {
					authorizers.add(authorizer);
				}
			}
			return new MultipleServiceAuthorizer(authorizers, true);
		}
	}

	public EAIResourceRepository getRepository() {
		return repository;
	}

}

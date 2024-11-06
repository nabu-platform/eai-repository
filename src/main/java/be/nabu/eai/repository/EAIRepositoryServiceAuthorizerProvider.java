/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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

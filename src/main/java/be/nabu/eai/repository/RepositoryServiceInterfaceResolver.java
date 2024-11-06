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

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;
import be.nabu.libs.services.api.ServiceInterface;

public class RepositoryServiceInterfaceResolver implements DefinedServiceInterfaceResolver {

	private Repository repository;
	
	public RepositoryServiceInterfaceResolver(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public DefinedServiceInterface resolve(String id) {
		Artifact resolved = repository.resolve(id);
		if (resolved instanceof DefinedServiceInterface) {
			return (DefinedServiceInterface) resolved;
		}
		else if (resolved instanceof DefinedService) {
			ServiceInterface iface = ((DefinedService) resolved).getServiceInterface();
			if (!(iface instanceof DefinedServiceInterface)) {
				iface = new ServiceInterfaceFromDefinedService((DefinedService) resolved);
			}
			return (DefinedServiceInterface) iface;
		}
		else if (resolved == null) {
			for (DefinedServiceInterfaceResolver resolver : repository.getArtifacts(DefinedServiceInterfaceResolver.class)) {
				DefinedServiceInterface resolvedIface = resolver.resolve(id);
				if (resolvedIface != null) {
					return resolvedIface;
				}
			}
		}
		return null;
	}
}

package be.nabu.eai.repository;

import java.util.HashMap;
import java.util.Map;

import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPRequestAuthenticator;
import be.nabu.libs.http.api.HTTPRequestAuthenticatorProvider;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;

public class RepositoryHTTPRequestAuthenticatorProvider implements HTTPRequestAuthenticatorProvider {

	private Repository repository;
	private Map<String, DefinedService> cache = new HashMap<String, DefinedService>();
	
	public RepositoryHTTPRequestAuthenticatorProvider(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public HTTPRequestAuthenticator getRequestAuthenticator(String name) {
		if (!cache.containsKey(name)) {
			String interfaceId = "be.nabu.libs.http.api.HTTPRequestAuthenticator.authenticate";
			DefinedService found = null;
			services: for (DefinedService service : repository.getArtifacts(DefinedService.class)) {
				// interfaces themselves are also services, don't count them though
				if (service instanceof DefinedServiceInterface) {
					continue;
				}
				ServiceInterface serviceInterface = service.getServiceInterface();
				while (serviceInterface != null) {
					if (serviceInterface instanceof DefinedServiceInterface) {
						if (interfaceId.equals(((DefinedServiceInterface) serviceInterface).getId())) {
							// once we found a service that actually implements the specification, we need to check the type which is registered in the node properties
							Entry entry = repository.getEntry(service.getId());
							if (entry != null) {
								Map<String, String> properties = entry.getNode().getProperties();
								if (properties != null && name.equals(properties.get("authenticationType"))) {
									found = service;
									break services;
								}
							}
							break;
						}
					}
					serviceInterface = serviceInterface.getParent();
				}
			}
			if (found != null) {
				synchronized(cache) {
					cache.put(name, found);
				}
			}
		}
		DefinedService service = cache.get(name);
		if (service != null) {
			// we reresolve the service in development in case there are updates
			if (EAIResourceRepository.isDevelopment()) {
				service = (DefinedService) repository.resolve(service.getId());
			}
			return POJOUtils.newProxy(HTTPRequestAuthenticator.class, service, repository, SystemPrincipal.ROOT);
		}
		else {
			return null;
		}
	}
}

/*
* Copyright (C) 2026 Alexander Verbruggen
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceLevelAgreement;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceLevelAgreement;
import be.nabu.libs.services.api.ServiceLevelAgreementListProvider;
import be.nabu.libs.services.api.ServiceLevelAgreementProvider;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;

public class RepositoryServiceLevelAgreementProvider implements ServiceLevelAgreementProvider {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private static final ServiceInterface SLA_LIST_INTERFACE = MethodServiceInterface.wrap(ServiceLevelAgreementListProvider.class, "getAllAgreements");
	private static final ThreadLocal<Boolean> loading = new ThreadLocal<Boolean>();
	private EAIResourceRepository repository;
	private volatile List<DefinedService> providers;
	private volatile Map<String, List<ServiceLevelAgreement>> agreementsByServiceId;

	public RepositoryServiceLevelAgreementProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<ServiceLevelAgreement> getAgreementsFor(Service service) {
		if (!(service instanceof DefinedService) || Boolean.TRUE.equals(loading.get())) {
			return null;
		}
		Map<String, List<ServiceLevelAgreement>> agreementsByServiceId = this.agreementsByServiceId;
		if (agreementsByServiceId == null) {
			synchronized(this) {
				agreementsByServiceId = this.agreementsByServiceId;
				if (agreementsByServiceId == null) {
					loading.set(true);
					try {
						agreementsByServiceId = loadAgreements();
						this.agreementsByServiceId = agreementsByServiceId;
					}
					finally {
						loading.remove();
					}
				}
			}
		}
		return agreementsByServiceId.get(((DefinedService) service).getId());
	}

	public void reset() {
		agreementsByServiceId = null;
		providers = null;
	}

	public List<DefinedServiceLevelAgreement> getAllAgreements() {
		Map<String, List<ServiceLevelAgreement>> agreementsByServiceId = this.agreementsByServiceId;
		if (agreementsByServiceId == null) {
			synchronized(this) {
				agreementsByServiceId = this.agreementsByServiceId;
				if (agreementsByServiceId == null) {
					loading.set(true);
					try {
						agreementsByServiceId = loadAgreements();
						this.agreementsByServiceId = agreementsByServiceId;
					}
					finally {
						loading.remove();
					}
				}
			}
		}
		List<DefinedServiceLevelAgreement> all = new ArrayList<DefinedServiceLevelAgreement>();
		for (Map.Entry<String, List<ServiceLevelAgreement>> entry : agreementsByServiceId.entrySet()) {
			for (ServiceLevelAgreement agreement : entry.getValue()) {
				if (agreement instanceof DefinedServiceLevelAgreement) {
					all.add((DefinedServiceLevelAgreement) agreement);
				}
			}
		}
		return all;
	}

	private Map<String, List<ServiceLevelAgreement>> loadAgreements() {
		Map<String, List<ServiceLevelAgreement>> loaded = new HashMap<String, List<ServiceLevelAgreement>>();
		for (DefinedService providerService : getProviders()) {
			try {
				ServiceLevelAgreementListProvider provider = POJOUtils.newProxy(ServiceLevelAgreementListProvider.class, providerService, repository, SystemPrincipal.ROOT);
				List<DefinedServiceLevelAgreement> agreements = provider.getAllAgreements();
				if (agreements != null) {
					for (DefinedServiceLevelAgreement agreement : agreements) {
						if (agreement != null && agreement.getServiceId() != null) {
							List<ServiceLevelAgreement> existing = loaded.get(agreement.getServiceId());
							if (existing == null) {
								existing = new ArrayList<ServiceLevelAgreement>();
								loaded.put(agreement.getServiceId(), existing);
							}
							existing.add(agreement);
						}
					}
				}
			}
			catch (Exception e) {
				logger.warn("Could not load service level agreements from provider: " + providerService.getId(), e);
			}
		}
		Map<String, List<ServiceLevelAgreement>> immutable = new HashMap<String, List<ServiceLevelAgreement>>();
		for (Map.Entry<String, List<ServiceLevelAgreement>> entry : loaded.entrySet()) {
			immutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
		}
		return immutable;
	}

	private List<DefinedService> getProviders() {
		List<DefinedService> providers = this.providers;
		if (providers == null) {
			synchronized(this) {
				providers = this.providers;
				if (providers == null) {
					providers = new ArrayList<DefinedService>();
					for (DefinedService service : repository.getServices()) {
						if (POJOUtils.isImplementation(service, SLA_LIST_INTERFACE)) {
							providers.add(service);
						}
					}
					this.providers = providers;
				}
			}
		}
		return providers;
	}
}

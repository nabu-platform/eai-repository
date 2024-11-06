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
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.api.ClusteredServer;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.cluster.api.ClusterInstance;
import be.nabu.libs.events.api.EventTarget;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.services.ListableServiceContext;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.FeaturedExecutionContext;
import be.nabu.libs.services.api.ForkableExecutionContext;
import be.nabu.libs.services.api.SecurityContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.TransactionContext;
import be.nabu.libs.services.api.UpgradeableSecurityContext;

public class EAIExecutionContext implements ForkableExecutionContext, FeaturedExecutionContext {

	private TransactionContext transactionContext = new EAITransactionContext();
	private SecurityContext securityContext;
	private EAIResourceRepository repository;
	private boolean isDebug;
	private ListableServiceContext serviceContext;
	private Token token;
	private List<Token> alternatives;
	private List<String> enabledFeatures;
	// you can disable events for a particular execution context
	private boolean disableEvents;
	
	public EAIExecutionContext(EAIResourceRepository repository, Token token, boolean isDebug, Token...alternatives) {
		this.repository = repository;
		this.token = token;
		this.isDebug = isDebug;
		this.securityContext = new EAISecurityContext();
		this.alternatives = Arrays.asList(alternatives);
		this.serviceContext = repository.getServiceContext();
	}
	
	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public ServiceContext getServiceContext() {
		return serviceContext;
	}

	@Override
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	@Override
	public boolean isDebug() {
		return isDebug;
	}

	public EAIResourceRepository getRepository() {
		return repository;
	}

	@Override
	public MetricInstance getMetricInstance(String id) {
		return repository.getMetricInstance(id);
	}
	
	public class EAISecurityContext implements UpgradeableSecurityContext {
		
		@Override
		public Token getToken() {
			return token;
		}

		@Override
		public List<Token> getAlternateTokens() {
			return alternatives;
		}

		@Override
		public RoleHandler getRoleHandler() {
			return repository.getRoleHandler();
		}

		@Override
		public PermissionHandler getPermissionHandler() {
			return repository.getPermissionHandler();
		}

		@Override
		public void upgrade(Token token, Token...alternateTokens) {
			EAIExecutionContext.this.token = token;
			EAIExecutionContext.this.alternatives = alternateTokens == null ? new ArrayList<Token>() : Arrays.asList(alternateTokens);
		}

		@Override
		public Device getDevice() {
			// if we have a request, attempt to get it from there!
			Object currentRequest = RequestProcessor.getCurrentRequest();
			if (currentRequest instanceof HTTPRequest) {
				return HTTPUtils.getDevice(null, token, true, ((HTTPRequest) currentRequest).getContent().getHeaders());
			}
			return UpgradeableSecurityContext.super.getDevice();
		}
	}

	@Override
	public ExecutionContext fork() {
		EAIExecutionContext context = new EAIExecutionContext(repository, token, isDebug, alternatives.toArray(new Token[alternatives.size()]));
		context.enabledFeatures = new ArrayList<String>(getEnabledFeatures());
		return context;
	}

	@Override
	public EventTarget getEventTarget() {
		return disableEvents == false ? repository.getComplexEventDispatcher() : null;
	}

	@Override
	public List<String> getEnabledFeatures() {
		if (enabledFeatures == null) {
			enabledFeatures = repository.getEnabledFeatures(getSecurityContext().getToken());
		}
		return enabledFeatures;
	}

	public void setEnabledFeatures(List<String> enabledFeatures) {
		this.enabledFeatures = enabledFeatures;
	}

	@Override
	public ClusterInstance getCluster() {
		return repository.getServiceRunner() instanceof ClusteredServer ? ((ClusteredServer) repository.getServiceRunner()).getCluster() : null;
	}

	public boolean isDisableEvents() {
		return disableEvents;
	}
	public void setDisableEvents(boolean disableEvents) {
		this.disableEvents = disableEvents;
	}
	
}

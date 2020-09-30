package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventTarget;
import be.nabu.libs.metrics.api.MetricInstance;
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
	}

	@Override
	public ExecutionContext fork() {
		EAIExecutionContext context = new EAIExecutionContext(repository, token, isDebug, alternatives.toArray(new Token[alternatives.size()]));
		context.enabledFeatures = new ArrayList<String>(getEnabledFeatures());
		return context;
	}

	@Override
	public EventTarget getEventTarget() {
		return repository.getComplexEventDispatcher();
	}

	@Override
	public List<String> getEnabledFeatures() {
		if (enabledFeatures == null) {
			enabledFeatures = repository.getEnabledFeatures();
		}
		return enabledFeatures;
	}
}

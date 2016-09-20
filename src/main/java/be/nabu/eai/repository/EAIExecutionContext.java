package be.nabu.eai.repository;

import java.util.Arrays;
import java.util.List;

import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.services.ListableServiceContext;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.SecurityContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.TransactionContext;

public class EAIExecutionContext implements ExecutionContext {

	private TransactionContext transactionContext = new EAITransactionContext();
	private SecurityContext securityContext;
	private EAIResourceRepository repository;
	private boolean isDebug;
	private ListableServiceContext serviceContext;
	private Token token;
	private List<Token> alternatives;
	
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
	
	public class EAISecurityContext implements SecurityContext {
		
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
	}
}

package be.nabu.eai.repository;

import java.security.Principal;

import be.nabu.libs.services.SimpleSecurityContext;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.SecurityContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.TransactionContext;

public class EAIExecutionContext implements ExecutionContext {

	private TransactionContext transactionContext = new EAITransactionContext();
	private SecurityContext securityContext;
	private EAIResourceRepository repository;
	private boolean isDebug;
	
	public EAIExecutionContext(EAIResourceRepository repository, Principal principal, boolean isDebug) {
		this.repository = repository;
		this.isDebug = isDebug;
		this.securityContext = new SimpleSecurityContext(principal);
	}
	
	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public ServiceContext getServiceContext() {
		// TODO: need to wrap services with an intelligent wrapper that allows for service stacks, security checks,...
		return repository.getServiceContext();
	}

	@Override
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	@Override
	public boolean isDebug() {
		return isDebug;
	}
	
}

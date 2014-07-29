package be.nabu.eai.repository;

import java.security.Principal;

import be.nabu.libs.services.SimpleSecurityContext;
import be.nabu.libs.services.SimpleTransactionContext;
import be.nabu.libs.services.api.SecurityContext;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.ServiceRuntime;
import be.nabu.libs.services.api.TransactionContext;

public class EAIRepositoryRuntime implements ServiceRuntime {

	private TransactionContext transactionContext = new SimpleTransactionContext();
	private SecurityContext securityContext;
	private EAIResourceRepository repository;
	
	public EAIRepositoryRuntime(EAIResourceRepository repository, Principal principal) {
		this.repository = repository;
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
	
}

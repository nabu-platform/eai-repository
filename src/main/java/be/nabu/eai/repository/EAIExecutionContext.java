package be.nabu.eai.repository;

import java.security.Principal;

import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.services.ListableServiceContext;
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
	private ListableServiceContext serviceContext;
	
	public EAIExecutionContext(EAIResourceRepository repository, Principal principal, boolean isDebug) {
		this.repository = repository;
		this.isDebug = isDebug;
		this.securityContext = new SimpleSecurityContext(principal);
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
}

package be.nabu.eai.repository;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import be.nabu.eai.repository.api.Repository;

public class RepositoryThreadFactory implements ThreadFactory {
	
	private Repository repository;
	private ThreadFactory parent;
	private boolean daemonize;

	public RepositoryThreadFactory(Repository repository) {
		this(repository, false);
	}
	
	public RepositoryThreadFactory(Repository repository, boolean daemonize) {
		this.repository = repository;
		this.daemonize = daemonize;
		this.parent = Executors.defaultThreadFactory();
	}

	@Override
	public Thread newThread(Runnable runnable) {
		Thread thread = this.parent.newThread(runnable);
		thread.setDaemon(daemonize);
		thread.setContextClassLoader(repository.getClassLoader());
		return thread;
	}
	
}

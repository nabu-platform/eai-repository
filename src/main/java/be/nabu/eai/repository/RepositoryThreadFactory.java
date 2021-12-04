package be.nabu.eai.repository;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;

import be.nabu.eai.repository.api.Repository;

public class RepositoryThreadFactory implements ThreadFactory, ForkJoinWorkerThreadFactory {
	
	private Repository repository;
	private ThreadFactory parent;
	private boolean daemonize;
	private String name;

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
		if (name != null) {
			thread.setName(name);
		}
		return thread;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		thread.setDaemon(daemonize);
		thread.setContextClassLoader(repository.getClassLoader());
		if (name != null) {
			thread.setName(name);
		}
		return thread;
	}
}

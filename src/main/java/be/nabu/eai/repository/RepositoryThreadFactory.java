package be.nabu.eai.repository;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import be.nabu.eai.repository.api.Repository;

public class RepositoryThreadFactory implements ThreadFactory {
	
	private Repository repository;
	private ThreadFactory parent;

	public RepositoryThreadFactory(Repository repository) {
		this.repository = repository;
		this.parent = Executors.defaultThreadFactory();
	}

	@Override
	public Thread newThread(Runnable runnable) {
		Thread thread = this.parent.newThread(runnable);
		thread.setContextClassLoader(repository.getClassLoader());
		return thread;
	}
}

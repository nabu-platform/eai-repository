package be.nabu.eai.repository;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

import be.nabu.eai.repository.api.Repository;

public class RepositoryForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

	private Repository repository;
	
	public RepositoryForkJoinWorkerThreadFactory(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		ForkJoinWorkerThread thread = new ForkJoinWorkerThread(pool){};
		thread.setContextClassLoader(repository.getClassLoader());
		return thread;
	}
	
}

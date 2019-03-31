package be.nabu.eai.repository.api;

import java.util.concurrent.ExecutorService;

public interface ExecutorServiceProvider {
	public ExecutorService getExecutorService();
}

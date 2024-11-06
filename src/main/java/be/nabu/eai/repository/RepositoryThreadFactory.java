/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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

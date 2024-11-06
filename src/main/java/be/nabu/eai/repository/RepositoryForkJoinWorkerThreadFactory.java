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

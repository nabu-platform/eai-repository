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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheProvider;

public class EAIRepositoryCacheProvider implements CacheProvider {

	private EAIResourceRepository repository;

	public EAIRepositoryCacheProvider(EAIResourceRepository repository) {
		this.repository = repository;
	}

	@Override
	public Cache get(String name) throws IOException {
		for (CacheProvider artifact : repository.getArtifacts(CacheProvider.class)) {
			Cache cache = artifact.get(name);
			if (cache != null) {
				return cache;
			}
		}
		return null;
	}

	@Override
	public void remove(String name) throws IOException {
		for (CacheProvider artifact : repository.getArtifacts(CacheProvider.class)) {
			artifact.remove(name);
		}
	}
	
	public Collection<String> getCaches() {
		List<String> caches = new ArrayList<String>();
		for (CacheProviderArtifact artifact : repository.getArtifacts(CacheProviderArtifact.class)) {
			caches.addAll(artifact.getCaches());
		}
		return caches;
	}
}

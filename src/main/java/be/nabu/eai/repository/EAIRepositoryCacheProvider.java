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

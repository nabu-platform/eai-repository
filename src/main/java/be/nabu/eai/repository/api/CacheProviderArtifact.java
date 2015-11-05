package be.nabu.eai.repository.api;

import java.util.Collection;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.cache.api.CacheRefresher;
import be.nabu.libs.cache.api.DataSerializer;

public interface CacheProviderArtifact extends Artifact, CacheProvider {
	public Cache create(String artifactId, long maxTotalSize, long maxEntrySize, long cacheTimeout, DataSerializer<?> keySerializer, DataSerializer<?> valueSerializer, CacheRefresher refresher);
	public Collection<String> getCaches();
}

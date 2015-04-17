package be.nabu.eai.repository.util;

import be.nabu.eai.repository.api.Node;

public class NodeUtils {
	/**
	 * "eager" or (default) "lazy"
	 */
	public static final String LOAD_TYPE = "services.load";
	/**
	 * How long to cache for
	 */
	public static final String CACHE_TIMEOUT = "services.cache.timeout";
	/**
	 * Whether or not to refresh the cache when it expires
	 */
	public static final String CACHE_REFRESH = "services.cache.refresh";

	public static boolean isEager(Node node) {
		String loadType = node.getProperties() != null ? node.getProperties().get(LOAD_TYPE) : null;
		return "eager".equalsIgnoreCase(loadType);
	}
	
	public static Long getCacheTimeout(Node node) {
		String cacheTimeout = node.getProperties() != null ? node.getProperties().get(CACHE_TIMEOUT) : null;
		return cacheTimeout == null ? null : Long.parseLong(cacheTimeout);
	}
	
	public static boolean shouldRefreshCache(Node node) {
		String cacheRefresh = node.getProperties() != null ? node.getProperties().get(CACHE_REFRESH) : null;
		return cacheRefresh != null && cacheRefresh.equalsIgnoreCase("true");
	}
}

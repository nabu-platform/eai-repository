package be.nabu.eai.repository.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.cache.api.CacheEntry;
import be.nabu.libs.cache.api.ExplorableCache;
import be.nabu.libs.http.api.server.Session;

public class CacheSession implements Session {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private CacheSessionProvider provider;
	private String sessionId;
	private Map<String, Object> values;

	CacheSession(CacheSessionProvider provider, Map<String, Object> values, String sessionId) {
		this.provider = provider;
		this.values = values;
		this.sessionId = sessionId;
	}
	
	@Override
	public Iterator<String> iterator() {
		List<String> keys = new ArrayList<String>();
		if (provider.getCache() instanceof ExplorableCache) {
			for (CacheEntry entry : ((ExplorableCache) provider.getCache()).getEntries()) {
				try {
					keys.add((String) entry.getKey());
				}
				catch (IOException e) {
					logger.error("Could not retrieve cache entry", e);
				}
			}
		}
		return keys.iterator();
	}

	@Override
	public String getId() {
		return sessionId;
	}

	@Override
	public Object get(String name) {
		return values.get(name);
	}

	@Override
	public void set(String name, Object value) {
		values.put(name, value);
		provider.persist(sessionId, values);
	}

	@Override
	public void destroy() {
		provider.destroy(sessionId);
	}

}

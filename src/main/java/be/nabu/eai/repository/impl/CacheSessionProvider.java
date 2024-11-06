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

package be.nabu.eai.repository.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.api.server.SessionProvider;

public class CacheSessionProvider implements SessionProvider {

	private Cache cache;

	public CacheSessionProvider(Cache cache) {
		this.cache = cache;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Session getSession(String sessionId) {
		try {
			Map<String, Object> values = (Map<String, Object>) cache.get(sessionId);
			if (values != null) {
				return new CacheSession(this, values, sessionId);
			}
			else {
				return null;
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Session newSession() {
		String sessionId = generateId();
		Map<String, Object> values = new HashMap<String, Object>();
		try {
			if (!cache.put(sessionId, values)) {
				throw new RuntimeException("Could not create session");
			}
			return new CacheSession(this, values, sessionId);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	void persist(String sessionId, Map<String, Object> values) {
		try {
			cache.put(sessionId, values);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	void destroy(String sessionId) {
		try {
			cache.clear(sessionId);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Cache getCache() {
		return cache;
	}

	/**
	 * The id that is generated must be non-predictable, otherwise it creates a surface for attack
	 * UUIDs are uniquely suited as they are globally unique and contain a secure random component, just make sure we use the correct type or we expose hardware information (MAC)
	 * The default implementation generates a type 4
	 */
	private String generateId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	@Override
	public void prune() {
		// do nothing for now
	}

}

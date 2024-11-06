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

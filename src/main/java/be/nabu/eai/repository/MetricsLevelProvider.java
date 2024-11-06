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
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.metrics.api.GroupLevelProvider;

public class MetricsLevelProvider implements GroupLevelProvider {

	private static Logger logger = LoggerFactory.getLogger(MetricsLevelProvider.class);
	private String id;
	private static Properties metricsProperties;

	public MetricsLevelProvider(String id) {
		this.id = id;
	}
	
	@Override
	public Integer getLevel(String group) {
		return getLevelFor(id, group);
	}

	public static Integer getLevelFor(String id, String group) {
		Properties properties = getMetricsProperties();
		// find a specific setting for this id
		String property = properties.getProperty(id + ":" + group);
		// find a generic setting for this id
		if (property == null) {
			// default to 1
			property = properties.getProperty("*:" + group, "1");
		}
		return Integer.parseInt(property);
	}

	public void set(String group, int level) {
		Properties properties = getMetricsProperties();
		synchronized(properties) {
			properties.put(id + ":" + group, level);
		}
	}
	
	public static Properties getMetricsProperties() {
		if (metricsProperties == null) {
			synchronized(MetricsLevelProvider.class) {
				if (metricsProperties == null) {
					Properties properties = new Properties();
					InputStream input = MetricsLevelProvider.class.getClassLoader().getResourceAsStream("metrics.properties");
					if (input != null) {
						try {
							try {
								properties.load(input);
							}
							finally {
								input.close();
							}
						}
						catch (IOException e) {
							logger.error("Could not load metrics file", e);
						}
					}
					metricsProperties = properties;
				}
			}
		}
		return metricsProperties;
	}
}

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
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

public class EAIRepositoryClassLoader extends ClassLoader {
	
	static {
		ClassLoader.registerAsParallelCapable();
	}
		
	private EAIResourceRepository repository;

	public EAIRepositoryClassLoader(EAIResourceRepository repository) {
		super(repository.getClass().getClassLoader());
		this.repository = repository;
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return super.findClass(name);
		}
		catch (ClassNotFoundException e) {
			// try locally
		}
		Class<?> clazz = repository.loadClass(name);
		if (clazz == null) {
			throw new ClassNotFoundException(name);
		}
		return clazz;
	}
	
	@Override
	public URL getResource(String name) {
		URL url = getParent().getResource(name);
		if (url == null) {
			url = repository.getResource(name);
		}
		return url;
	}
	
	@Override
	public InputStream getResourceAsStream(String name) {
		URL resource = getResource(name);
		try {
			return resource == null ? null : resource.openStream();
		}
		catch (IOException e) {
			return null;
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Set<URL> resources = new LinkedHashSet<URL>();
		if (name.equals("META-INF/services/javax.xml.parsers.DocumentBuilderFactory")) {
			return Collections.enumeration(resources);
		}
		Enumeration<URL> enumeration = getParent().getResources(name);
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				resources.add(enumeration.nextElement());
			}
		}
		resources.addAll(repository.getResources(name));
		return Collections.enumeration(resources);
	}
	
	
}

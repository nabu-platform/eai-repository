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

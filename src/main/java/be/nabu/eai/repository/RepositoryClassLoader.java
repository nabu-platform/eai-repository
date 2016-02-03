package be.nabu.eai.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RepositoryClassLoader extends ClassLoader {

	private EAIResourceRepository repository;
	private List<String> blacklist;

	public RepositoryClassLoader(EAIResourceRepository repository, ClassLoader parent, String...blacklist) {
		super(parent);
		this.repository = repository;
		this.blacklist = blacklist == null ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(blacklist));
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		try {
			return getParent().loadClass(name);
		}
		catch (ClassNotFoundException e) {
			// try locally
		}
		Class<?> clazz = repository.loadClass(name, blacklist);
		if (clazz == null) {
			throw new ClassNotFoundException(name);
		}
		else {
			return clazz;
		}
	}
	
}

package be.nabu.eai.repository.artifacts.jaxb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class JAXBArtifact<T> implements Artifact {

	private ResourceContainer<?> directory;
	private String id;
	private String fileName;
	private Class<T> configurationClazz;
	private T configuration;
	private Repository repository;

	public JAXBArtifact(String id, ResourceContainer<?> directory, Repository repository, String fileName, Class<T> configurationClazz) {
		this.id = id;
		this.directory = directory;
		this.repository = repository;
		this.fileName = fileName;
		this.configurationClazz = configurationClazz;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	public T unmarshal(InputStream input) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(configurationClazz);
		return (T) context.createUnmarshaller().unmarshal(input);
	}
	
	public void marshal(T configuration, OutputStream output) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(configurationClazz);
		context.createMarshaller().marshal(configuration, output);
	}	

	public T getConfiguration() throws IOException {
		if (configuration == null) {
			synchronized(this) {
				if (configuration == null) {
					Resource target = directory.getChild(fileName);
					if (target == null) {
						try {
							configuration = configurationClazz.newInstance();
						}
						catch (InstantiationException e) {
							throw new RuntimeException(e);
						}
						catch (IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					}
					else {
						ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) target);
						try {
							configuration = unmarshal(IOUtils.toInputStream(readable));
						}
						catch (JAXBException e) {
							throw new IOException(e);
						}
						finally {
							readable.close();
						}
					}
				}
			}
		}
		return configuration;
	}
	
	public void save(ResourceContainer<?> directory) throws IOException {
		T configuration = getConfiguration();
		Resource target = directory.getChild(fileName);
		if (target == null) {
			target = ((ManageableContainer<?>) directory).create(fileName, "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
		try {
			marshal(configuration, IOUtils.toOutputStream(writable));
		}
		catch (JAXBException e) {
			throw new IOException(e);
		}
		finally {
			writable.close();
		}
	}

	public static boolean notNull(Object...objects) {
		if (objects == null) {
			return false;
		}
		for (Object object : objects) {
			if (object == null) {
				return false;
			}
		}
		return true;
	}

	public ResourceContainer<?> getDirectory() {
		return directory;
	}
	
	@Override
	public String toString() {
		return getId() + " [" + getClass().getName() + "]";
	}

	public Repository getRepository() {
		return repository;
	}
	
}

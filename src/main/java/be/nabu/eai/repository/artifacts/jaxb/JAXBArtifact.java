package be.nabu.eai.repository.artifacts.jaxb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.artifacts.api.LazyArtifact;
import be.nabu.libs.artifacts.api.LiveReloadable;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class JAXBArtifact<T> implements LazyArtifact, LiveReloadable {

	private ResourceContainer<?> directory;
	private String id;
	private String fileName;
	private Class<T> configurationClazz;
	private T configuration;
	private Repository repository;
	// false by default!
	private boolean canLiveReload = false;
	private Logger logger = LoggerFactory.getLogger(getClass());

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
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setAdapter(new ArtifactXMLAdapter(getRepository()));
		return (T) unmarshaller.unmarshal(input);
	}
	
	public void marshal(T configuration, OutputStream output) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(configurationClazz);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setAdapter(new ArtifactXMLAdapter(getRepository()));
		marshaller.marshal(configuration, output);
	}	

	public T getConfig() {
		try {
			return getConfiguration();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void mergeConfiguration(T configuration, boolean includeNull) {
		BeanInstance<T> newInstance = new BeanInstance<T>(configuration);
		BeanInstance<T> current = new BeanInstance<T>(getConfig());
		for (Element<?> element : TypeUtils.getAllChildren(newInstance.getType())) {
			Object value = newInstance.get(element.getName());
			if (value != null || includeNull) {
				current.set(element.getName(), value);
			}
		}
	}
	
	public T getConfiguration() throws IOException {
		if (configuration == null) {
			synchronized(this) {
				if (configuration == null) {
					loadConfigurationLive();
				}
			}
		}
		return configuration;
	}

	private void loadConfigurationLive() throws IOException {
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
				configuration = validateConfiguration(unmarshal(IOUtils.toInputStream(readable)));
			}
			catch (JAXBException e) {
				throw new IOException(e);
			}
			finally {
				readable.close();
			}
		}
	}
	
	private T validateConfiguration(T configuration) {
		validate(configuration);
		return configuration;
	}
	// we use artifact xml adapter in numerous places and we can't tell it which type of artifact though that information is generally hidden in the generics
	// we use this f*ed up meta-processing to figure out these problems and can't them
	// this can only occur with generics which are wiped at compile time, for single artifacts this will probably simply error out
	// we need another solution for that at some point...
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void validate(Object configuration) {
		try {
			for (Method method : configuration.getClass().getMethods()) {
				// if we are getting a collection
				if (method.getName().startsWith("get") && method.getParameterCount() == 0 && Collection.class.isAssignableFrom(method.getReturnType())) {
					// that are converted with artifacts
					XmlJavaTypeAdapter annotation = method.getAnnotation(XmlJavaTypeAdapter.class);
					if (annotation != null) {
						if (ArtifactXMLAdapter.class.isAssignableFrom(annotation.value())) {
							Type returnType = method.getGenericReturnType();
							if (returnType instanceof ParameterizedType) {
								Type result = ((ParameterizedType) returnType).getActualTypeArguments()[0];
								if (result instanceof Class) {
									Collection collection = (Collection) method.invoke(configuration);
									if (collection != null) {
										Iterator iterator = collection.iterator();
										while (iterator.hasNext()) {
											Object next = iterator.next();
											if (next != null && !((Class) result).isAssignableFrom(next.getClass())) {
												logger.info("Skipping incorrect entry in " + getId() + " (" + method.getName() + "): " + next + " is not of type " + result);
												iterator.remove();
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Could not standardize configuration", e);
		}
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

	@Override
	public void forceLoad() {
		try {
			getConfiguration();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void setConfig(T config) {
		this.configuration = config;
	}

	@Override
	public void liveReload() {
		try {
			loadConfigurationLive();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean canLiveReload() {
		return canLiveReload;
	}
	
}

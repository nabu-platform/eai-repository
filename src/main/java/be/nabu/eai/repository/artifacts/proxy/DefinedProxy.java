package be.nabu.eai.repository.artifacts.proxy;

import java.io.IOException;

import javax.xml.bind.JAXBException;

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

public class DefinedProxy implements Artifact {

	private ProxyConfiguration configuration;
	private String id;
	private ResourceContainer<?> directory;
	
	public DefinedProxy(String id, ResourceContainer<?> directory) {
		this.id = id;
		this.directory = directory;
	}
	
	@Override
	public String getId() {
		return id;
	}

	public ProxyConfiguration getConfiguration() throws IOException {
		if (configuration == null) {
			synchronized(this) {
				if (configuration == null) {
					Resource target = directory.getChild("proxy.xml");
					if (target == null) {
						configuration = new ProxyConfiguration();
					}
					else {
						ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) target);
						try {
							configuration = ProxyConfiguration.unmarshal(IOUtils.toInputStream(readable));
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
		ProxyConfiguration configuration = getConfiguration();
		Resource target = directory.getChild("proxy.xml");
		if (target == null) {
			target = ((ManageableContainer<?>) directory).create("proxy.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) target);
		try {
			configuration.marshal(IOUtils.toOutputStream(writable));
		}
		catch (JAXBException e) {
			throw new IOException(e);
		}
		finally {
			writable.close();
		}
	}

}

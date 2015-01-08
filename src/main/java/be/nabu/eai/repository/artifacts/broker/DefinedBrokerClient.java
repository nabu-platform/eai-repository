package be.nabu.eai.repository.artifacts.broker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.xml.bind.JAXBException;

import be.nabu.eai.broker.client.BrokerClient;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.tasks.TaskUtils;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.security.KeyStoreHandler;
import be.nabu.utils.security.SSLContextType;

public class DefinedBrokerClient implements Artifact {

	private static final String BROKER_POLL_INTERVAL = "be.nabu.eai.broker.pollInterval";
	private static final String BROKER_CONNECTION_TIMEOUT = "be.nabu.eai.broker.connectionTimeout";
	private static final String BROKER_SOCKET_TIMEOUT = "be.nabu.eai.broker.socketTimeout";
	private static final String BROKER_PROCESSING_POOL_SIZE = "be.nabu.eai.broker.processingPoolSize";
	private static final String BROKER_CONNECTION_POOL_SIZE = "be.nabu.eai.broker.connectionPoolSize";
	
	private String id;
	private BrokerClient brokerClient;
	private BrokerConfiguration configuration;
	private ResourceContainer<?> directory;

	public DefinedBrokerClient(String id, ResourceContainer<?> directory) { 
		this.id = id;
		this.directory = directory;
	}

	@Override
	public String getId() {
		return id;
	}

	public void save(ResourceContainer<?> directory) throws IOException {
		BrokerConfiguration configuration = getConfiguration();
		Resource target = directory.getChild("broker.xml");
		if (target == null) {
			target = ((ManageableContainer<?>) directory).create("broker.xml", "application/xml");
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
	
	public BrokerConfiguration getConfiguration() throws IOException {
		if (configuration == null) {
			synchronized(this) {
				if (configuration == null) {
					Resource target = directory.getChild("broker.xml");
					if (target == null) {
						configuration = new BrokerConfiguration();
					}
					else {
						ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) target);
						try {
							configuration = BrokerConfiguration.unmarshal(IOUtils.toInputStream(readable));
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
	
	public BrokerClient getBrokerClient() throws IOException {
		if (brokerClient == null) {
			synchronized(this) {
				if (brokerClient == null) {
					DefinedKeyStore keystore = getConfiguration().getKeystore();
					try {
						brokerClient = new BrokerClient(
							getConfiguration().getClientId() == null ? TaskUtils.generateManagerId() : getConfiguration().getClientId(),
							getConfiguration().getEndpoint(),
							getConfiguration().getUsername(), 
							getConfiguration().getPassword(), 
							getConfiguration().getEncoding() == null ? Charset.defaultCharset() : Charset.forName(getConfiguration().getEncoding()), 
							getConfiguration().getProcessingPoolSize() == null ? new Integer(System.getProperty(BROKER_PROCESSING_POOL_SIZE, "5")) : getConfiguration().getProcessingPoolSize(), 
							getConfiguration().getPollInterval() == null ? new Integer(System.getProperty(BROKER_POLL_INTERVAL, "1000")) : getConfiguration().getPollInterval(), 
							getConfiguration().getConnectionPoolSize() == null ? new Integer(System.getProperty(BROKER_CONNECTION_POOL_SIZE, "5")) : getConfiguration().getConnectionPoolSize(),
							getConfiguration().getConnectionTimeout() == null ? new Integer(System.getProperty(BROKER_CONNECTION_TIMEOUT, "60000")) : getConfiguration().getConnectionTimeout(), 
							getConfiguration().getSocketTimeout() == null ? new Integer(System.getProperty(BROKER_SOCKET_TIMEOUT, "60000")) : getConfiguration().getSocketTimeout(), 
							false, 
							keystore == null ? null : new KeyStoreHandler(keystore.getKeyStore().getKeyStore()).createContext(SSLContextType.TLS)
						);
					}
					catch (KeyManagementException e) {
						throw new RuntimeException(e);
					}
					catch (UnrecoverableKeyException e) {
						throw new RuntimeException(e);
					}
					catch (KeyStoreException e) {
						throw new RuntimeException(e);
					}
					catch (NoSuchAlgorithmException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return brokerClient;
	}
}

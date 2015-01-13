package be.nabu.eai.repository.artifacts.broker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import be.nabu.eai.broker.client.BrokerClient;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.tasks.TaskUtils;
import be.nabu.utils.security.KeyStoreHandler;
import be.nabu.utils.security.SSLContextType;

public class DefinedBrokerClient extends JAXBArtifact<BrokerConfiguration> {

	private static final String BROKER_CONNECTION_TIMEOUT = "be.nabu.eai.broker.connectionTimeout";
	private static final String BROKER_SOCKET_TIMEOUT = "be.nabu.eai.broker.socketTimeout";
	private static final String BROKER_PROCESSING_POOL_SIZE = "be.nabu.eai.broker.processingPoolSize";
	private static final String BROKER_CONNECTION_POOL_SIZE = "be.nabu.eai.broker.connectionPoolSize";
	private static final String BROKER_DEVIATION = "be.nabu.eai.broker.deviation";
	
	private BrokerClient brokerClient;

	public DefinedBrokerClient(String id, ResourceContainer<?> directory) {
		super(id, directory, "broker.xml", BrokerConfiguration.class);
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
							getConfiguration().getConnectionPoolSize() == null ? new Integer(System.getProperty(BROKER_CONNECTION_POOL_SIZE, "5")) : getConfiguration().getConnectionPoolSize(),
							getConfiguration().getConnectionTimeout() == null ? new Integer(System.getProperty(BROKER_CONNECTION_TIMEOUT, "60000")) : getConfiguration().getConnectionTimeout(), 
							getConfiguration().getSocketTimeout() == null ? new Integer(System.getProperty(BROKER_SOCKET_TIMEOUT, "60000")) : getConfiguration().getSocketTimeout(), 
							false,
							getConfiguration().getDeviation() == null ? new Double(System.getProperty(BROKER_DEVIATION, "0.2")) : getConfiguration().getDeviation(),
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

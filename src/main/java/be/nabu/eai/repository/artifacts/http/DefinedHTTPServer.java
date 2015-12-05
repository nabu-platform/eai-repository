package be.nabu.eai.repository.artifacts.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.security.KeyStoreHandler;
import be.nabu.utils.security.SSLContextType;

public class DefinedHTTPServer extends JAXBArtifact<DefinedHTTPServerConfiguration> implements StartableArtifact, StoppableArtifact {

	private static final String HTTP_IO_POOL_SIZE = "be.nabu.eai.http.ioPoolSize";
	private static final String HTTP_PROCESS_POOL_SIZE = "be.nabu.eai.http.processPoolSize";
	private Thread thread;
	private Repository repository;
	
	public DefinedHTTPServer(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, "httpServer.xml", DefinedHTTPServerConfiguration.class);
		this.repository = repository;
	}

	private HTTPServer server;
	
	@Override
	public void stop() throws IOException {
		getServer().stop();
	}

	@Override
	public void start() throws IOException {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					getServer().start();
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();
	}
	
	public HTTPServer getServer() {
		if (server == null) {
			synchronized(this) {
				if (server == null) {
					try {
						if (getConfiguration().getKeystore() == null) {
							server = HTTPServerUtils.newServer(
								getConfiguration().getPort() == null ? 80 : getConfiguration().getPort(), 
								getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_PROCESS_POOL_SIZE, "10")) : getConfiguration().getPoolSize(),
								new EventDispatcherImpl());
						}
						else {
							KeyStoreHandler keyStoreHandler = new KeyStoreHandler(getConfiguration().getKeystore().getKeyStore().getKeyStore());
							KeyManager[] keyManagers = keyStoreHandler.getKeyManagers();
							for (int i = 0; i < keyManagers.length; i++) {
								if (keyManagers[i] instanceof X509KeyManager) {
									keyManagers[i] = new ArtifactAwareKeyManager((X509KeyManager) keyManagers[i], repository, this);
								}
							}
							SSLContext context = SSLContext.getInstance(SSLContextType.TLS.toString());
							context.init(keyManagers, keyStoreHandler.getTrustManagers(), new SecureRandom());
							server = HTTPServerUtils.newServer(
								context,
								getConfiguration().getSslServerMode(),
								getConfiguration().getPort() == null ? 443 : getConfiguration().getPort(),
								getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_IO_POOL_SIZE, "5")) : getConfiguration().getPoolSize(),
								getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_PROCESS_POOL_SIZE, "10")) : getConfiguration().getPoolSize(),
								new EventDispatcherImpl()
							);
						}
						server.setExceptionFormatter(new RepositoryExceptionFormatter());
						if (!EAIResourceRepository.isDevelopment()) {
							server.getDispatcher().subscribe(HTTPResponse.class, HTTPServerUtils.ensureContentEncoding());
						}
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
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return server;
	}

	@Override
	public boolean isStarted() {
		return thread != null && thread.getState() != Thread.State.TERMINATED;
	}
}

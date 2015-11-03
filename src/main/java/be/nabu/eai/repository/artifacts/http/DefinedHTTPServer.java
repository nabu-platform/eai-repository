package be.nabu.eai.repository.artifacts.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.security.KeyStoreHandler;
import be.nabu.utils.security.SSLContextType;

public class DefinedHTTPServer extends JAXBArtifact<DefinedHTTPServerConfiguration> implements StartableArtifact, StoppableArtifact {

	private static final String HTTP_IO_POOL_SIZE = "be.nabu.eai.http.ioPoolSize";
	private static final String HTTP_PROCESS_POOL_SIZE = "be.nabu.eai.http.processPoolSize";
	private Thread thread;
	
	public DefinedHTTPServer(String id, ResourceContainer<?> directory) {
		super(id, directory, "httpServer.xml", DefinedHTTPServerConfiguration.class);
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
						server = getConfiguration().getKeystore() == null 
							? HTTPServerUtils.newServer(
								getConfiguration().getPort(), 
								getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_PROCESS_POOL_SIZE, "10")) : getConfiguration().getPoolSize())
							: HTTPServerUtils.newServer(
								getConfiguration().getKeystore() == null ? null : new KeyStoreHandler(getConfiguration().getKeystore().getKeyStore().getKeyStore()).createContext(SSLContextType.TLS),
								getConfiguration().getSslServerMode(),
								getConfiguration().getPort(),
								getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_IO_POOL_SIZE, "5")) : getConfiguration().getPoolSize(),
								getConfiguration().getPoolSize() == null ? new Integer(System.getProperty(HTTP_PROCESS_POOL_SIZE, "10")) : getConfiguration().getPoolSize()
						);
						server.setExceptionFormatter(new RepositoryExceptionFormatter());
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

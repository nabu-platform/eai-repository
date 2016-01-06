package be.nabu.eai.repository.artifacts.http;

import java.io.IOException;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.http.virtual.VirtualHostArtifact;
import be.nabu.utils.io.SSLServerMode;

/**
 * SSL is a problem for virtual hosts (https://wiki.apache.org/httpd/NameBasedSSLVHosts)
 * In a nutshell: virtual hosts work by checking the domain that was requested and routing to the appropriate application
 * However if the virtual hosts are protected by SSL, the handshake occurs way before any http request is sent and before the Host header signals which host is requested!
 * To combat this, SNI (server name indication) was added to TLS handshakes. SNI is supported in java as a client from 7 onwards and as a server from 8 onwards.
 * Most browsers have supported this extension for a number of years now so in general it should not pose a problem.
 * 
 * It is possible to set SNI matchers on the engine.getSSLParameters() but I'm not sure if that is necessary in order to get the requested SNI names in this part of the code.
 * 
 * This code is in part based on http://stackoverflow.com/questions/20807408/handling-multiple-certificates-in-nettys-ssl-handler-used-in-play-framework-1-2
 * And https://github.com/grahamedgecombe/netty-sni-example/
 * TODO: how important is it to signal the chosen webartifact to the "upper" layer? Meaning we choose the correct pipeline to perform the requests?
 * 		> it is theoretically possible to connect to a host using SNI a.com and then perform requests to b.com over that secure connection
 * 		> the only downside to this is if you are using client side certificates because it will allow you to bypass
 */
public class ArtifactAwareKeyManager extends X509ExtendedKeyManager {

	private Repository repository;
	private List<VirtualHostArtifact> virtualHosts;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private X509KeyManager parent;
	private DefinedHTTPServer server;
	
	public ArtifactAwareKeyManager(X509KeyManager parent, Repository repository, DefinedHTTPServer server) {
		this.parent = parent;
		this.repository = repository;
		this.server = server;
	}
	
	/**
	 * This method is used by the SSLEngine instance to determine which alias to use
	 */
	@Override
	public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
		List<VirtualHostArtifact> virtualHosts = getVirtualHosts();
		Iterator<VirtualHostArtifact> iterator = virtualHosts.iterator();
		while (iterator.hasNext()) {
			try {
				if (!server.equals(iterator.next().getConfiguration().getServer())) {
					iterator.remove();
				}
			}
			catch (Exception e) {
				logger.error("Could not load web artifact", e);
				iterator.remove();
			}
		}
		// no valid webartifacts
		if (virtualHosts.isEmpty()) {
			return null;
		}
		// just one, return that
		else if (virtualHosts.size() == 1) {
			try {
				return virtualHosts.get(0).getConfiguration().getKeyAlias();
			}
			catch (IOException e) {
				logger.error("Could not get alias", e);
				return null;
			}
		}
		// multiple, choose on the basis of the SNI host name (if any)
		else {
			try {
				if (SSLServerMode.NEED_CLIENT_CERTIFICATES.equals(server.getConfiguration().getSslServerMode())) {
					logger.error("Currently SNI-based resolving on multiple web artifacts is not supported if client certificates are turned on because we can't guarantee (yet) that the correct pipeline is chosen");
					return null;
				}
				ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();
				SNIHostName hostName = null;
				for (SNIServerName name : session.getRequestedServerNames()) {
					if (name.getType() == StandardConstants.SNI_HOST_NAME) {
						hostName = (SNIHostName) name;
						break;
					}
				}
				if (hostName == null) {
					logger.error("Multiple web artifacts on a secure connection but no SNI in the original request");
					return null;
				}
				for (VirtualHostArtifact artifact : getVirtualHosts()) {
					try {
						List<String> hosts = new ArrayList<String>();
						if (artifact.getConfiguration().getHost() != null) {
							hosts.add(artifact.getConfiguration().getHost());
						}
						if (artifact.getConfiguration().getAliases() != null) {
							hosts.addAll(artifact.getConfiguration().getAliases());
						}
						for (String host : hosts) {
							SNIHostName sniHostName = new SNIHostName(host);
							if (sniHostName.equals(hostName)) {
								if (artifact.getConfiguration().getKeyAlias() == null) {
									logger.error("No key alias set on virtual host: " + artifact.getId());
									return null;
								}
								return artifact.getConfiguration().getKeyAlias();
							}
						}
					}
					catch (Exception e) {
						logger.error("Could not check virtual host for SNI hostname matches", e);
					}
				}
				logger.error("Found multiple virtual hosts but none had a host that matches: " + hostName);
				return null;
			}
			catch (Exception e) {
				logger.error("Could not determine virtual host", e);
				return null;
			}
		}
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		return parent.getCertificateChain(alias);
	}
	@Override
	public PrivateKey getPrivateKey(String alias) {
		return parent.getPrivateKey(alias);
	}
	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return parent.getServerAliases(keyType, issuers);
	}

	private List<VirtualHostArtifact> getVirtualHosts() {
		if (virtualHosts == null || EAIResourceRepository.isDevelopment()) {
			synchronized(this) {
				if (virtualHosts == null || EAIResourceRepository.isDevelopment()) {
					List<VirtualHostArtifact> virtualHosts = new ArrayList<VirtualHostArtifact>();
					for (Node node : repository.getNodes(VirtualHostArtifact.class)) {
						try {
							virtualHosts.add((VirtualHostArtifact) node.getArtifact());
						}
						catch (Exception e) {
							logger.error("Could not load virtual host: " + node, e);
						}
					}
					this.virtualHosts = virtualHosts;
				}
			}
		}
		return virtualHosts;
	}

	/**
	 * Unsupported
	 */
	@Override
	public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
		throw new UnsupportedOperationException();
	}
	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		throw new UnsupportedOperationException();
	}
	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		throw new UnsupportedOperationException();
	}
	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		throw new UnsupportedOperationException();
	}

}

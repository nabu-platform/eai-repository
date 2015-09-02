package be.nabu.eai.repository.artifacts.web.rest;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class WebRestArtifact extends JAXBArtifact<WebRestArtifactConfiguration> implements StartableArtifact, StoppableArtifact, DefinedServiceInterface {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private EventSubscription<HTTPRequest, HTTPResponse> subscription;
	private ComplexType input, output;
	
	public WebRestArtifact(String id, ResourceContainer<?> directory) {
		super(id, directory, "webrestartifact.xml", WebRestArtifactConfiguration.class);
	}
	
	@Override
	public void stop() throws IOException {
		if (subscription != null) {
			subscription.unsubscribe();
			subscription = null;
		}
	}

	@Override
	public void start() throws IOException {
		if (getConfiguration().getWebArtifact() != null) {
			List<DefinedService> services = new ArrayList<DefinedService>();
			for (Node node : EAIResourceRepository.getInstance().getNodes(DefinedService.class)) {
				DefinedService service;
				try {
					service = (DefinedService) node.getArtifact();
					if (POJOUtils.isImplementation(service, this)) {
						services.add(service);
					}
				}
				catch (ParseException e) {
					logger.error("Could not load node: " + node, e);
				}
			}
			if (services.isEmpty()) {
				logger.error("No implementations found of REST interface: " + getId());
			}
			else {
				subscription = getConfiguration().getWebArtifact().getConfiguration().getHttpServer().getServer().getEventDispatcher().subscribe(HTTPRequest.class, new WebRestListener(getConfiguration(), this, services));
			}
		}
		else {
			logger.error("Could not start artifact " + getId() + ": no configured web artifact");
		}
	}

	@Override
	public void save(ResourceContainer<?> directory) throws IOException {
		super.save(directory);
		synchronized(this) {
			rebuildInterface();
		}
	}

	@Override
	public ComplexType getInputDefinition() {
		if (input == null) {
			synchronized(this) {
				if (output == null) {
					rebuildInterface();
				}
			}
		}
		return input;
	}

	@Override
	public ComplexType getOutputDefinition() {
		if (output == null) {
			synchronized(this) {
				if (output == null) {
					rebuildInterface();
				}
			}
		}
		return output;
	}

	@Override
	public ServiceInterface getParent() {
		return null;
	}
	
	private void rebuildInterface() {
		Structure input = new Structure();
		Structure query = new Structure();
		Structure header = new Structure();
		Structure session = new Structure();
		Structure cookie = new Structure();
		Structure path = new Structure();
		try {
			if (getConfiguration().getQueryParameters() != null && !getConfiguration().getQueryParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getQueryParameters().split("[\\s,]+")) {
					query.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), query, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
				}
			}
			if (getConfiguration().getHeaderParameters() != null && !getConfiguration().getHeaderParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getHeaderParameters().split("[\\s,]+")) {
					header.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), header, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
				}
			}
			if (getConfiguration().getSessionParameters() != null && !getConfiguration().getSessionParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getSessionParameters().split("[\\s,]+")) {
					session.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), session));
				}
			}
			if (getConfiguration().getCookieParameters() != null && !getConfiguration().getCookieParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getCookieParameters().split("[\\s,]+")) {
					cookie.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), cookie, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
				}
			}
			for (String name : GlueListener.analyzePath(getConfiguration().getPath()).getParameters()) {
				path.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), path));
			}
		}
		catch (IOException e) {
			logger.error("Can not rebuild interface", e);
		}
	}
}

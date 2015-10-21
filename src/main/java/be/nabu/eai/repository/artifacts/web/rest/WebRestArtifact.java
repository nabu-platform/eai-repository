package be.nabu.eai.repository.artifacts.web.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class WebRestArtifact extends JAXBArtifact<WebRestArtifactConfiguration> implements DefinedServiceInterface {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private Structure input, output;
	
	public WebRestArtifact(String id, ResourceContainer<?> directory) {
		super(id, directory, "webrestartifact.xml", WebRestArtifactConfiguration.class);
	}
	
	@Override
	public void save(ResourceContainer<?> directory) throws IOException {
		synchronized(this) {
			rebuildInterface();
		}
		super.save(directory);
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
		// reuse references so everything gets auto-updated
		Structure input = this.input == null ? new Structure() : clean(this.input);
		Structure output = this.output == null ? new Structure() : clean(this.output);
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
				input.add(new ComplexElementImpl("query", query, input));
			}
			if (getConfiguration().getHeaderParameters() != null && !getConfiguration().getHeaderParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getHeaderParameters().split("[\\s,]+")) {
					header.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), header, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
				}
				input.add(new ComplexElementImpl("header", header, input));
			}
			if (getConfiguration().getSessionParameters() != null && !getConfiguration().getSessionParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getSessionParameters().split("[\\s,]+")) {
					session.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), session));
				}
				input.add(new ComplexElementImpl("session", session, input));
			}
			if (getConfiguration().getCookieParameters() != null && !getConfiguration().getCookieParameters().trim().isEmpty()) {
				for (String name : getConfiguration().getCookieParameters().split("[\\s,]+")) {
					cookie.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), cookie, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
				}
				input.add(new ComplexElementImpl("cookie", cookie, input));
			}
			if (getConfiguration().getPath() != null) {
				for (String name : GlueListener.analyzePath(getConfiguration().getPath()).getParameters()) {
					path.add(new SimpleElementImpl<String>(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), path));
				}
				if (path.iterator().hasNext()) {
					input.add(new ComplexElementImpl("path", path, input));
				}
			}
			if (getConfiguration().getInput() != null) {
				input.add(new ComplexElementImpl("content", (ComplexType) getConfiguration().getInput(), input));
			}
			if (getConfiguration().getOutput() != null) {
				output.add(new ComplexElementImpl("content", (ComplexType) getConfiguration().getOutput(), output));
			}
			this.input = input;
			this.output = output;
		}
		catch (IOException e) {
			logger.error("Can not rebuild interface", e);
		}
	}

	private static Structure clean(Structure input) {
		for (Element<?> element : TypeUtils.getAllChildren(input)) {
			input.remove(element);
		}
		return input;
	}
}

package be.nabu.eai.repository.api;

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.authentication.api.Token;

public interface FeatureConfigurator {
	@WebResult(name = "enabledFeatures")
	public List<String> getEnabledFeatures(@WebParam(name = "token") Token token);
	// the context that this configurator operates in
	// if no context, it operates everywhere
	// it can be a comma separated list of contexts
	public String getContext();
	// when the feature configuration was last modified
	public Date getLastModified();
}

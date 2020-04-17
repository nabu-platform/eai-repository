package be.nabu.eai.repository.api;

import java.util.Date;
import java.util.List;

public interface FeatureConfigurator {
	public List<String> getEnabledFeatures();
	// the context that this configurator operates in
	// if no context, it operates everywhere
	// it can be a comma separated list of contexts
	public String getContext();
	// when the feature configuration was last modified
	public Date getLastModified();
}

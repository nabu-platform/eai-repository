package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.services.api.ExecutionContext;

public interface FeaturedExecutionContext extends ExecutionContext {
	// features are uniquely identified by their name, this is the name of the feature
	public List<String> getEnabledFeatures();
}

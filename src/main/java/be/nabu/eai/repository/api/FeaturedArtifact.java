package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface FeaturedArtifact extends Artifact {
	public List<Feature> getAvailableFeatures();
}

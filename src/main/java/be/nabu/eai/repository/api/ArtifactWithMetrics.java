package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface ArtifactWithMetrics extends Artifact {

	public List<ArtifactMetricDefinition> getMetricDescriptions();
	
	public static class ArtifactMetricDefinition {
		private String format, description;

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}

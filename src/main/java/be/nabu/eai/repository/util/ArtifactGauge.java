package be.nabu.eai.repository.util;

import be.nabu.libs.metrics.api.MetricGauge;

public class ArtifactGauge implements MetricGauge {
	private String name, artifactId;
	private long value;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	@Override
	public long getValue() {
		return value;
	}
	public void setValue(long value) {
		this.value = value;
	}
}

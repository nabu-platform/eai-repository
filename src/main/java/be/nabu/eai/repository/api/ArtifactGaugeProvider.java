package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.eai.repository.util.ArtifactGauge;

public interface ArtifactGaugeProvider {
	public List<ArtifactGauge> getGauges();
}

package be.nabu.eai.repository.api;

import java.util.Date;

import javax.validation.constraints.NotNull;

public interface FeatureDescription {
	@NotNull
	public String getName();
	public String getContext();
	public Date getLastModified();
	public Boolean getEnabled();
}

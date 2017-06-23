package be.nabu.eai.repository.api;

import java.util.Collection;

public interface Documented {
	public String getTitle();
	public String getDescription();
	public Collection<String> getTags();
	public String getMimeType();
}

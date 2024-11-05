package be.nabu.eai.repository.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Documented {
	public String getTitle();
	public String getDescription();
	public Collection<String> getTags();
	public String getMimeType();
	// get nested fragments
	public default List<Documented> getFragments() {
		return new ArrayList<Documented>();
	}
}

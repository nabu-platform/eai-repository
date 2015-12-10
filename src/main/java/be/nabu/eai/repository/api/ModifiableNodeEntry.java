package be.nabu.eai.repository.api;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface ModifiableNodeEntry {
	public void updateNode(List<String> references) throws IOException;
	public void updateNodeContext(String environmentId, long version, Date lastModified) throws IOException;
}

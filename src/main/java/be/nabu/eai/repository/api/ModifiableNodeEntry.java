package be.nabu.eai.repository.api;

import java.io.IOException;
import java.util.List;

public interface ModifiableNodeEntry {
	public void updateNode(List<String> references) throws IOException;
}

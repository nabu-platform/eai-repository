package be.nabu.eai.repository.api;

import java.nio.charset.Charset;

import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.events.api.EventDispatcher;

public interface Repository {
	public RepositoryEntry getRoot();
	public Charset getCharset();
	public EventDispatcher getEventDispatcher();
	public Node getNode(String id);
}

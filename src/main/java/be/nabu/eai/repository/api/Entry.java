package be.nabu.eai.repository.api;

import be.nabu.libs.resources.api.ResourceContainer;

public interface Entry extends ResourceContainer<Entry> {
	public String getId();
	public boolean isLeaf();
	public boolean isEditable();
	public boolean isNode();
	public Node getNode();
	public Repository getRepository();
	public void refresh();
	
	@Override
	public Entry getParent();
}

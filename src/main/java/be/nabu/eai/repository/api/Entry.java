package be.nabu.eai.repository.api;

import be.nabu.libs.resources.api.ResourceContainer;

public interface Entry extends ResourceContainer<Entry> {
	public String getId();
	public boolean isLeaf();
	public boolean isEditable();
	public boolean isNode();
	public Node getNode();
	public Repository getRepository();
	public void refresh(boolean recursive);
	
	@Override
	public Entry getParent();
	
	public default Collection getCollection() { return null; }
	public default boolean isCollection() {
		return getCollection() != null;
	}
}

package be.nabu.eai.repository.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import be.nabu.eai.repository.api.DynamicEntry;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.resources.api.Resource;

public class MemoryEntry implements ModifiableEntry, DynamicEntry {

	private String id;
	private String name;
	private Entry parent;
	private Repository repository;
	private Node node;
	private Map<String, Entry> children;
	private String originator;
	
	public MemoryEntry(Repository repository, Entry parent, Node node, String id, String name, Entry...children) {
		this(parent.getId(), repository, parent, node, id, name, children);
	}
	
	public MemoryEntry(String originator, Repository repository, Entry parent, Node node, String id, String name, Entry...children) {
		this.originator = originator;
		this.repository = repository;
		this.node = node;
		this.id = id;
		this.name = name;
		this.parent = parent;
		this.children = new LinkedHashMap<String, Entry>();
		for (Entry child : children) {
			this.children.put(child.getName(), child);
		}
	}
	
	@Override
	public Entry getChild(String name) {
		return children.get(name);
	}

	@Override
	public String getContentType() {
		return isNode()
			? "application/vnd-nabu-" + getNode().getArtifactClass().getName()
			: Resource.CONTENT_TYPE_DIRECTORY;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Entry getParent() {
		return parent;
	}

	@Override
	public Iterator<Entry> iterator() {
		return new ArrayList<Entry>(children.values()).iterator();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isLeaf() {
		return children.isEmpty();
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public boolean isNode() {
		return node != null;
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public void addChildren(Entry...children) {
		for (Entry child : children) {
			this.children.put(child.getName(), child);
		}
	}

	@Override
	public void removeChildren(String...children) {
		for (String child : children) {
			this.children.remove(child);
		}
	}

	@Override
	public void refresh(boolean recursive) {
		// do nothing
	}

	@Override
	public String getOriginatingArtifact() {
		return originator;
	}

	@Override
	public String toString() {
		return getId() + "[memory]";
	}
}

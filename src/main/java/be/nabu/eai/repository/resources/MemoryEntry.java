package be.nabu.eai.repository.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
	private List<Entry> children;
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
		this.children = new ArrayList<Entry>(Arrays.asList(children));
	}
	
	@Override
	public Entry getChild(String name) {
		for (Entry child : children) {
			if (child.getName().equals(name)) {
				return child;
			}
		}
		return null;
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
		return children.iterator();
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
		this.children.addAll(Arrays.asList(children));
	}

	@Override
	public void removeChildren(String...children) {
		Iterator<Entry> iterator = iterator();
		List<String> names = Arrays.asList(children);
		while (iterator.hasNext()) {
			if (names.contains(iterator.next().getName())) {
				iterator.remove();
			}
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

}

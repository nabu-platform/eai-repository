package be.nabu.eai.repository.api;

public interface ModifiableEntry extends Entry {
	public void addChildren(Entry...children);
	public void removeChildren(String...children);
}

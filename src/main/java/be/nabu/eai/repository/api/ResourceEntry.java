package be.nabu.eai.repository.api;

import be.nabu.libs.resources.api.ResourceContainer;

public interface ResourceEntry<T extends ResourceEntry<T>> extends Entry<T> {
	@Override
	public ResourceRepository getRepository();
	public ResourceContainer<?> getContainer();
}

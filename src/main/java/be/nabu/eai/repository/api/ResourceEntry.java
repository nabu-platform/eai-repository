package be.nabu.eai.repository.api;

import be.nabu.libs.resources.api.ResourceContainer;

public interface ResourceEntry extends Entry {
	@Override
	public ResourceRepository getRepository();
	public ResourceContainer<?> getContainer();
}

package be.nabu.eai.repository.api;

import be.nabu.libs.resources.api.ResourceContainer;

public interface ResourceRepository extends Repository {
	public boolean isInternal(ResourceContainer<?> container);
	public boolean isValidName(ResourceContainer<?> parent, String name);
}

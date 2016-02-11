package be.nabu.eai.repository.events;

import be.nabu.libs.property.api.Property;

public interface PropertyUpdatedEvent {
	public Property<?> getProperty();
	public Object getOldValue();
	public Object getNewValue();
	public Object getTarget();
}

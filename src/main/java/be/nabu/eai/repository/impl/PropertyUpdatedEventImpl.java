package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.events.PropertyUpdatedEvent;
import be.nabu.libs.property.api.Property;

public class PropertyUpdatedEventImpl implements PropertyUpdatedEvent {

	private Property<?> property;
	private Object oldValue, newValue, target;
	
	public PropertyUpdatedEventImpl(Object target, Property<?> property, Object newValue, Object oldValue) {
		this.target = target;
		this.property = property;
		this.newValue = newValue;
		this.oldValue = oldValue;
	}
	@Override
	public Property<?> getProperty() {
		return property;
	}
	public void setProperty(Property<?> property) {
		this.property = property;
	}
	@Override
	public Object getOldValue() {
		return oldValue;
	}
	public void setOldValue(Object oldValue) {
		this.oldValue = oldValue;
	}
	@Override
	public Object getNewValue() {
		return newValue;
	}
	public void setNewValue(Object newValue) {
		this.newValue = newValue;
	}
	@Override
	public Object getTarget() {
		return target;
	}
	public void setTarget(Object target) {
		this.target = target;
	}
	
	
}

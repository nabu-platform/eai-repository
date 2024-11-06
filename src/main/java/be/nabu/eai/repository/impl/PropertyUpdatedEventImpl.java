/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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

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

package be.nabu.eai.repository.api;

import be.nabu.libs.validator.api.Validation;

public interface Notification extends Validation<String> {
	/**
	 * Arbitrary properties that can be attached to notifications
	 */
	public Object getProperties();
	
	/**
	 * The type of notification
	 */
	public String getType();
	
	/**
	 * The user that triggered the notification
	 */
	public String getRealm();
	public String getAlias();
	
	/**
	 * The device that triggered the notification
	 */
	public String getDeviceId();
	
	/**
	 * A unique identifier for this notification
	 */
	public String getIdentifier();
}

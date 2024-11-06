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

package be.nabu.eai.repository;

import java.security.Principal;

public class InternalPrincipal implements Principal {

	/**
	 * The name of the user
	 */
	private String name;
	
	/**
	 * The id of the source artifact that set the principal
	 * For example when a trigger starts a service, it needs a principal
	 * This is an internal one set with the id of the trigger
	 */
	private String sourceId;
	
	public InternalPrincipal(String name, String sourceId) {
		this.name = name;
		this.sourceId = sourceId;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public String getSourceId() {
		return sourceId;
	}
}

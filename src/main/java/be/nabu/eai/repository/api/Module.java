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

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

/**
 * The id of the artifact is what will be used to define dependencies
 */
public interface Module extends Artifact {
	/**
	 * A logical name of the module
	 */
	public String getName();
	/**
	 * The version of the module
	 */
	public String getVersion();
	/**
	 * A description of the module
	 */
	public String getDescription();
	/**
	 * The modules this module depends on to function
	 */
	public List<Module> getDependencies();
}

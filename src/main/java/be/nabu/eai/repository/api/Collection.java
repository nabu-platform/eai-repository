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

public interface Collection {
	/**
	 * We want broad categories of collection types (like project) which can have specific attached to it it
	 * In such a broad type you can have a subtype to further distinguish yourself
	 */
	public String getType();
	/**
	 * You can have a subtype
	 * For example the main type might be "project" while the subtype is "standard" or "web" or ... whatever types of projects you want
	 */
	public String getSubType();
	/**
	 * We can set a human readable name for the node
	 */
	public String getName();
	/**
	 * We can set a description, this is meant to be read by business people
	 */
	public String getDescription();
	/**
	 * A comment is meant to be read by technical people 
	 */
	public String getComment();
	/**
	 * A summary is meant to be read by outsiders
	 */
	public String getSummary();
	/**
	 * You can tag a node so we can group them together based on context
	 */
	public List<String> getTags();
	/**
	 * Versioning and deployment are done at the project level
	 * By having quick access to the version, we can do things like compare environments
	 */
	public String getVersion();
	/**
	 * A small icon at 16x16
	 */
	public String getSmallIcon();
	
	/**
	 * An medium icon at 32x32
	 */
	public String getMediumIcon();
	
	/**
	 * An large icon at 64x64
	 */
	public String getLargeIcon();
}

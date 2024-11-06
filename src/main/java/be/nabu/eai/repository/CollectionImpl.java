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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.repository.api.Collection;

@XmlRootElement(name = "collection")
public class CollectionImpl implements Collection {
	private String name, description, summary, type, subType, comment, version, smallIcon, mediumIcon, largeIcon;
	private List<String> tags;
	
	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	@Override
	public List<String> getTags() {
		return tags;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	@Override
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String getSubType() {
		return subType;
	}
	public void setSubType(String subType) {
		this.subType = subType;
	}

	@Override
	public String getSmallIcon() {
		return smallIcon;
	}
	public void setSmallIcon(String smallIcon) {
		this.smallIcon = smallIcon;
	}

	@Override
	public String getMediumIcon() {
		return mediumIcon;
	}
	public void setMediumIcon(String mediumIcon) {
		this.mediumIcon = mediumIcon;
	}

	@Override
	public String getLargeIcon() {
		return largeIcon;
	}
	public void setLargeIcon(String largeIcon) {
		this.largeIcon = largeIcon;
	}

}

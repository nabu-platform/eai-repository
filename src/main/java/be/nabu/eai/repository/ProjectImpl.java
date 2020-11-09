package be.nabu.eai.repository;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.repository.api.Project;

@XmlRootElement(name = "project")
public class ProjectImpl implements Project {
	private String name, description, summary, type, comment, version;
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
	
}

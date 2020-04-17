package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.api.Feature;

public class FeatureImpl implements Feature {
	private String name, description;

	public FeatureImpl() {
		// auto
	}
	public FeatureImpl(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}

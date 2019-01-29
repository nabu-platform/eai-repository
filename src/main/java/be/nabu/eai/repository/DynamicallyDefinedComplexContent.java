package be.nabu.eai.repository;

import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;

public class DynamicallyDefinedComplexContent implements ComplexContent {

	private ComplexType originalType;
	private ComplexType dynamicType;
	private ComplexContent instance;

	public DynamicallyDefinedComplexContent(ComplexType originalType, ComplexType dynamicType) {
		this.originalType = originalType;
		this.dynamicType = dynamicType;
		this.instance = originalType.newInstance();
	}
	
	@Override
	public ComplexType getType() {
		return dynamicType;
	}

	@Override
	public void set(String path, Object value) {
		instance.set(path, value);
	}

	@Override
	public Object get(String path) {
		return instance.get(path);
	}

	public ComplexType getOriginalType() {
		return originalType;
	}

}

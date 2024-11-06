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

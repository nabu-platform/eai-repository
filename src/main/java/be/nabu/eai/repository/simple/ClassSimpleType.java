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

package be.nabu.eai.repository.simple;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.BaseMarshallableSimpleType;

@SuppressWarnings("rawtypes")
public class ClassSimpleType extends BaseMarshallableSimpleType<Class> implements Unmarshallable<Class> {

	public ClassSimpleType() {
		super(Class.class);
	}

	@Override
	public String marshal(Class arg0, Value<?>...arg1) {
		return arg0 == null ? null : arg0.getName();
	}
	
	@Override
	public String getName(Value<?>... arg0) {
		return "class";
	}

	@Override
	public String getNamespace(Value<?>... arg0) {
		return null;
	}

	@Override
	public Class unmarshal(String arg0, Value<?>... arg1) {
		try {
			return (Class) (arg0 == null ? null : Thread.currentThread().getContextClassLoader().loadClass(arg0));
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}

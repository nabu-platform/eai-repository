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

package be.nabu.eai.repository.simple;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.BaseMarshallableSimpleType;

@SuppressWarnings("rawtypes")
public class ClassSimpleType<T extends Class> extends BaseMarshallableSimpleType<T> implements Unmarshallable<T> {

	@SuppressWarnings("unchecked")
	public ClassSimpleType() {
		super((Class<T>) Class.class);
	}

	@Override
	public String marshal(T arg0, Value<?>...arg1) {
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

	@SuppressWarnings("unchecked")
	@Override
	public T unmarshal(String arg0, Value<?>... arg1) {
		try {
			return (T) (arg0 == null ? null : Thread.currentThread().getContextClassLoader().loadClass(arg0));
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}

package be.nabu.eai.repository.artifacts.simpleType;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.api.Enumerator;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.properties.ActualTypeProperty;

public class SimpleTypeEnumerator implements Enumerator {

	@Override
	public List<?> enumerate() {
		return new ArrayList<SimpleType<?>>(new ActualTypeProperty().getEnumerations());
	}

}

package be.nabu.eai.repository.impl;

import java.util.Date;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;

// we want to make sure all events are timestamped in the correct order
public class CreatedDateEnricher implements EventEnricher {

	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		if (!(object instanceof ComplexContent)) {
			object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
		}
		if (object != null) {
			// if we have a field called "sessionId", we enrich it
			if (((ComplexContent) object).getType().get("created") != null) {
				if (((ComplexContent) object).get("created") == null) {
					((ComplexContent) object).set("created", new Date());
				}
			}
		}
		return null;
	}

}

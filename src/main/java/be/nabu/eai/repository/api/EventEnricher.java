package be.nabu.eai.repository.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

public interface EventEnricher {
	// you can return a completely different object if necessary
	@WebResult(name = "event")
	public Object enrich(@WebParam(name = "event") Object object);
}

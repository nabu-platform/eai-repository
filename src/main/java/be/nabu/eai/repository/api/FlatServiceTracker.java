package be.nabu.eai.repository.api;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

public interface FlatServiceTracker {
	public void track(@NotNull @WebParam(name="isBefore") boolean isBefore, @NotNull @WebParam(name="serviceId") String service, @WebParam(name="step") String step, @WebParam(name="exception") Exception exception);
}

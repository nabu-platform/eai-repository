package be.nabu.eai.repository.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

public interface Translator {
	@WebResult(name = "translation")
	public String translate(@NotNull @WebParam(name = "id") String id, @NotNull @WebParam(name = "key") String key, @WebParam(name = "language") String language);
}

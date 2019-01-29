package be.nabu.eai.repository.api;

import java.util.List;

import javax.jws.WebResult;

public interface LanguageProvider {
	@WebResult(name = "languages")
	public List<String> getSupportedLanguages();
}

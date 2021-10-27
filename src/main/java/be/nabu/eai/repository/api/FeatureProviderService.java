package be.nabu.eai.repository.api;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.authentication.api.Token;

public interface FeatureProviderService {
	@WebResult(name = "features")
	public List<FeatureDescription> features(@WebParam(name = "token") Token token);
}

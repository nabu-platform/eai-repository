package be.nabu.eai.repository.api;

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public interface NotificationProvider extends Artifact {
	public void notify(
			@WebParam(name = "identifier") @NotNull String identifier,
			@WebParam(name = "context") @NotNull List<String> context,
			@WebParam(name = "created") @NotNull Date created,
			@WebParam(name = "severity") @NotNull Severity severity, 
			@WebParam(name = "message") String message, 
			@WebParam(name = "description") String description,
			@WebParam(name = "type") String type,
			@WebParam(name = "code") Integer code,
			@WebParam(name = "alias") String alias,
			@WebParam(name = "realm") String realm,
			@WebParam(name = "deviceId") String deviceId,
			@WebParam(name = "properties") List<KeyValuePair> properties);
}

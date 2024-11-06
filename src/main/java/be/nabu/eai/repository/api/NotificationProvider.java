/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository.api;

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

import be.nabu.libs.artifacts.api.Artifact;
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
			@WebParam(name = "code") String code,
			@WebParam(name = "alias") String alias,
			@WebParam(name = "realm") String realm,
			@WebParam(name = "deviceId") String deviceId,
			@WebParam(name = "properties") Object properties);
}

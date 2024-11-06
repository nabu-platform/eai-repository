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

package be.nabu.eai.repository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

@XmlRootElement
public class Notification implements be.nabu.eai.repository.api.Notification {

	private Severity severity;
	private List<String> context;
	private String code;
	private String message, description;
	private Object properties;
	private String type;
	private Date created = new Date();
	private String serviceContext, realm, alias, deviceId;
	private String identifier;
	
	public Notification() {
		this(UUID.randomUUID().toString().replace("-", ""));
	}
	
	public Notification(String identifier) {
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime != null) {
			setServiceContext(ServiceUtils.getServiceContext(runtime));
			// not simply saving token because of serializability, jaxb can not handle interfaces
			// additionally, the token may contain a lot of inherently unserializable data, we only need the identity of the user
			Token token = runtime.getExecutionContext().getSecurityContext().getToken();
			if (token != null) {
				setAlias(token.getName());
				setRealm(token.getRealm());
			}
		}
		// use a specific identifier
		this.identifier = identifier;
	}
	
	@Override
	public Severity getSeverity() {
		return severity;
	}
	public void setSeverity(Severity severity) {
		this.severity = severity;
	}
	
	@Override
	public List<String> getContext() {
		return context;
	}
	public void setContext(List<String> context) {
		this.context = context;
	}
	
	@Override
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	@XmlTransient
	public Object getProperties() {
		return properties;
	}
	public void setProperties(Object properties) {
		this.properties = properties;
	}
	
	public static String format(Exception e) {
		StringWriter writer = new StringWriter();
		PrintWriter printer = new PrintWriter(writer);
		e.printStackTrace(printer);
		printer.flush();
		return writer.toString();
	}
	
	@Override
	public Date getCreated() {
		return created;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
	@Override
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getServiceContext() {
		return serviceContext;
	}
	public void setServiceContext(String serviceContext) {
		this.serviceContext = serviceContext;
	}

	@Override
	public String getRealm() {
		return realm;
	}
	public void setRealm(String realm) {
		this.realm = realm;
	}

	@Override
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

}

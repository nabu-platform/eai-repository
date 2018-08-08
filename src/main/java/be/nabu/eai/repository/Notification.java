package be.nabu.eai.repository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

@XmlRootElement
public class Notification implements Validation<String> {

	private Severity severity;
	private List<String> context;
	private Integer code;
	private String message, description;
	private Object properties;
	private String type;
	private Date created = new Date();
	private String serviceContext, realm, alias;
	
	public Notification() {
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
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
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
	
	public Date getCreated() {
		return created;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
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

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

}

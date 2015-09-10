package be.nabu.eai.repository.artifacts.web;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.api.RestServiceFilter;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "webArtifact")
@XmlType(propOrder = { "httpServer", "path", "charset", "passwordAuthenticationService", "secretAuthenticationService", "permissionService", "roleService", "tokenValidatorService", "restServices" })
public class WebArtifactConfiguration {
	
	private DefinedHTTPServer httpServer;
	private String path;
	private String charset;
	
	private DefinedService passwordAuthenticationService, secretAuthenticationService;
	private DefinedService permissionService;
	private DefinedService roleService;
	private DefinedService tokenValidatorService;
	private List<DefinedService> restServices;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedHTTPServer getHttpServer() {
		return httpServer;
	}

	public void setHttpServer(DefinedHTTPServer httpServer) {
		this.httpServer = httpServer;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.PasswordAuthenticator.authenticate")
	public DefinedService getPasswordAuthenticationService() {
		return passwordAuthenticationService;
	}
	public void setPasswordAuthenticationService(
			DefinedService passwordAuthenticationService) {
		this.passwordAuthenticationService = passwordAuthenticationService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.SecretAuthenticator.authenticate")
	public DefinedService getSecretAuthenticationService() {
		return secretAuthenticationService;
	}
	public void setSecretAuthenticationService(
			DefinedService secretAuthenticationService) {
		this.secretAuthenticationService = secretAuthenticationService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.PermissionHandler.hasPermission")
	public DefinedService getPermissionService() {
		return permissionService;
	}
	public void setPermissionService(DefinedService permissionService) {
		this.permissionService = permissionService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.RoleHandler.hasRole")
	public DefinedService getRoleService() {
		return roleService;
	}
	public void setRoleService(DefinedService roleService) {
		this.roleService = roleService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.TokenValidator.isValid")
	public DefinedService getTokenValidatorService() {
		return tokenValidatorService;
	}
	public void setTokenValidatorService(DefinedService tokenValidatorService) {
		this.tokenValidatorService = tokenValidatorService;
	}

	@RestServiceFilter
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedService> getRestServices() {
		return restServices;
	}
	public void setRestServices(List<DefinedService> restServices) {
		this.restServices = restServices;
	}
}
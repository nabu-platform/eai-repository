package be.nabu.eai.repository.artifacts.web;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.repository.artifacts.http.DefinedHTTPServer;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "webArtifact")
public class WebArtifactConfiguration {
	
	private DefinedHTTPServer httpServer;
	private String path;
	private String charset;
	
	private DefinedService authenticationService;
	private DefinedService permissionService;
	private DefinedService roleService;
	private DefinedService tokenValidatorService;

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
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.Authenticator.authenticate")
	public DefinedService getAuthenticationService() {
		return authenticationService;
	}
	public void setAuthenticationService(DefinedService authenticationService) {
		this.authenticationService = authenticationService;
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
}

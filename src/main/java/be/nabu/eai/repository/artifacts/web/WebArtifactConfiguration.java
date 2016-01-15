package be.nabu.eai.repository.artifacts.web;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.eai.repository.artifacts.http.virtual.VirtualHostArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "webArtifact")
@XmlType(propOrder = { "virtualHost", "realm", "path", "charset", "allowBasicAuthentication", "passwordAuthenticationService", "secretAuthenticationService", "permissionService", "roleService", "tokenValidatorService", "trackerService", "translationService", "languageProviderService", "whitelistedCodes", "cacheProvider", "maxTotalSessionSize", "maxSessionSize", "sessionTimeout", "webFragments" })
public class WebArtifactConfiguration {

	private CacheProviderArtifact cacheProvider;
	private Long maxTotalSessionSize, maxSessionSize, sessionTimeout; 
	private VirtualHostArtifact virtualHost;
	private String path;
	private String charset;
	private String realm;
	private String whitelistedCodes;
	
	private DefinedService passwordAuthenticationService, secretAuthenticationService;
	private DefinedService permissionService;
	private DefinedService roleService;
	private DefinedService tokenValidatorService;
	private DefinedService trackerService;
	private DefinedService translationService;
	private DefinedService languageProviderService;
	private Boolean allowBasicAuthentication;
	private List<WebFragment> webFragments;
	
	@EnvironmentSpecific
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
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.Translator.translate")
	public DefinedService getTranslationService() {
		return translationService;
	}
	public void setTranslationService(DefinedService translationService) {
		this.translationService = translationService;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.UserLanguageProvider.getLanguage")	
	public DefinedService getLanguageProviderService() {
		return languageProviderService;
	}
	public void setLanguageProviderService(DefinedService languageProviderService) {
		this.languageProviderService = languageProviderService;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.PasswordAuthenticator.authenticate")
	public DefinedService getPasswordAuthenticationService() {
		return passwordAuthenticationService;
	}
	public void setPasswordAuthenticationService(DefinedService passwordAuthenticationService) {
		this.passwordAuthenticationService = passwordAuthenticationService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.SecretAuthenticator.authenticate")
	public DefinedService getSecretAuthenticationService() {
		return secretAuthenticationService;
	}
	public void setSecretAuthenticationService(DefinedService secretAuthenticationService) {
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

	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.FlatServiceTracker.track")
	public DefinedService getTrackerService() {
		return trackerService;
	}
	public void setTrackerService(DefinedService serviceTrackerService) {
		this.trackerService = serviceTrackerService;
	}

	@EnvironmentSpecific
	public Boolean getAllowBasicAuthentication() {
		return allowBasicAuthentication;
	}
	public void setAllowBasicAuthentication(Boolean allowBasicAuthentication) {
		this.allowBasicAuthentication = allowBasicAuthentication;
	}

	public String getRealm() {
		return realm;
	}
	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getWhitelistedCodes() {
		return whitelistedCodes;
	}
	public void setWhitelistedCodes(String whitelistedCodes) {
		this.whitelistedCodes = whitelistedCodes;
	}

	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public CacheProviderArtifact getCacheProvider() {
		return cacheProvider;
	}
	public void setCacheProvider(CacheProviderArtifact cacheProvider) {
		this.cacheProvider = cacheProvider;
	}

	@EnvironmentSpecific
	public Long getMaxTotalSessionSize() {
		return maxTotalSessionSize;
	}
	public void setMaxTotalSessionSize(Long maxTotalSessionSize) {
		this.maxTotalSessionSize = maxTotalSessionSize;
	}

	@EnvironmentSpecific
	public Long getMaxSessionSize() {
		return maxSessionSize;
	}
	public void setMaxSessionSize(Long maxSessionSize) {
		this.maxSessionSize = maxSessionSize;
	}

	@EnvironmentSpecific
	public Long getSessionTimeout() {
		return sessionTimeout;
	}
	public void setSessionTimeout(Long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<WebFragment> getWebFragments() {
		return webFragments;
	}
	public void setWebFragments(List<WebFragment> webFragments) {
		this.webFragments = webFragments;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public VirtualHostArtifact getVirtualHost() {
		return virtualHost;
	}
	public void setVirtualHost(VirtualHostArtifact virtualHost) {
		this.virtualHost = virtualHost;
	}
}

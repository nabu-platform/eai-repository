package be.nabu.eai.repository.api;

import java.security.cert.X509Certificate;

public interface LicenseManager {
	public static final String MAIN = "server";
	public boolean isLicensed(String topic);
	public void addLicense(X509Certificate...certificates);
}

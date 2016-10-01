package be.nabu.eai.repository.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.LicenseManager;
import be.nabu.utils.security.BCSecurityUtils;
import be.nabu.utils.security.SecurityUtils;

public class LicenseManagerImpl implements LicenseManager {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private List<X509Certificate> certificates = new ArrayList<X509Certificate>();
	private X509Certificate rootCA, licenseCA;
	
	public LicenseManagerImpl() {
		this.rootCA = getRootCA();
		this.licenseCA = getLicenseCA();
	}
	
	@Override
	public boolean isLicensed(String topic) {
		Date now = new Date();
		Iterator<X509Certificate> iterator = certificates.iterator();
		while (iterator.hasNext()) {
			X509Certificate certificate = iterator.next();
			if (certificate.getNotAfter().before(now)) {
				iterator.remove();
			}
			else if (certificate.getNotBefore().after(now)) {
				continue;
			}
			else {
				Map<String, String> parts = SecurityUtils.getParts(certificate.getSubjectX500Principal());
				String commonName = parts.get("CN");
				if (commonName != null) {
					// can sign multiple modules in one license
					for (String signed : commonName.split("[\\s]*,[\\s]*")) {
						if (signed.equals(topic) || topic.matches(signed.replace(".", "\\.").replace("*", ".*"))) {
							checkValidityPeriod(certificate);
							return true;
						}
					}
				}
			}
		}
		logger.warn("No license found for: " + topic);
		return false;
	}

	@Override
	public void addLicense(X509Certificate...certificates) {
		for (X509Certificate certificate : certificates) {
			if (certificate.getNotAfter().before(new Date())) {
				logger.warn("Expired license " + certificate.getSubjectX500Principal());
				continue;
			}
			try {
				// add the certificate and the intermediate
				CertStore certStore = BCSecurityUtils.createCertificateStore(new X509Certificate[] { certificate, licenseCA });
				// make sure we validate the certificate itself
				X509CertSelector selector = new X509CertSelector();
				selector.setCertificate(certificate);
				PKIXCertPathBuilderResult result = BCSecurityUtils.createPKIXPath(selector, certStore, rootCA);
				if (result != null && result.getCertPath() != null) {
					X500Principal subjectX500Principal = certificate.getSubjectX500Principal();
					logger.info("Activated license: " + SecurityUtils.getParts(subjectX500Principal).get("CN"));
					checkValidityPeriod(certificate);
					this.certificates.add(certificate);
				}
			}
			catch (Exception e) {
				logger.warn("Invalid license: " + certificate.getSubjectX500Principal() + " signed by " + certificate.getIssuerX500Principal());
			}
		}
	}
	
	private void checkValidityPeriod(X509Certificate certificate) {
		Date notAfter = certificate.getNotAfter();
		long duration = notAfter.getTime() - new Date().getTime();
		if (duration < 0) {
			logger.warn("License: " + certificate.getSubjectX500Principal() + " is expired");
		}
		else if (duration < 1000l*60*60*24) {
			logger.warn("License: " + certificate.getSubjectX500Principal() + " will expire in: " + (duration / (1000l * 60)) + "min");
		}
		else if (duration < 1000l*60*60*24*7) {
			logger.warn("License: " + certificate.getSubjectX500Principal() + " will expire in less than a week");
		}
		else if (duration < 1000l*60*60*24*30) {
			logger.warn("License: " + certificate.getSubjectX500Principal() + " will expire in less than a month");
		}
	}

	public static X509Certificate getRootCA() {
		String content = "-----BEGIN CERTIFICATE-----\n" + 
			"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\n" + 
			"-----END CERTIFICATE-----\n";
		try {
			return SecurityUtils.parseCertificate(new ByteArrayInputStream(content.getBytes(Charset.forName("ASCII"))));
		}
		catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static X509Certificate getLicenseCA() {
		String content = "-----BEGIN CERTIFICATE-----\n" + 
			"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\n" + 
			"-----END CERTIFICATE-----\n";
		try {
			return SecurityUtils.parseCertificate(new ByteArrayInputStream(content.getBytes(Charset.forName("ASCII"))));
		}
		catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}
}

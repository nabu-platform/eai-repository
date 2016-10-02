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
			"6iL1x2zhhYxgRijlnmQ2aIe2I4aYJXb8DzuLPZk=\n" + 
			"-----END CERTIFICATE-----\n";
		try {
			return SecurityUtils.parseCertificate(new ByteArrayInputStream(content.getBytes(Charset.forName("ASCII"))));
		}
		catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}
}

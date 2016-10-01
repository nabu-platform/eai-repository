package be.nabu.eai.repository.api;

public interface LicensedRepository extends Repository {
	public LicenseManager getLicenseManager();
}

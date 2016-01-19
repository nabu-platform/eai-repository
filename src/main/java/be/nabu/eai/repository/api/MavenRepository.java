package be.nabu.eai.repository.api;

import java.net.URI;

import be.nabu.libs.maven.api.DomainRepository;

public interface MavenRepository extends Repository {
	public void unloadMavenArtifact(be.nabu.libs.maven.api.Artifact artifact);
	public void loadMavenArtifact(be.nabu.libs.maven.api.Artifact artifact);
	public be.nabu.libs.maven.api.DomainRepository getMavenRepository();
	public URI getMavenRoot();
	public void register(DomainRepository domainRepository);
	public void unregister(DomainRepository domainRepository);
}

package be.nabu.eai.repository.impl;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ContextualArtifact;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;

public class RepositoryArtifactResolver<T extends Artifact> {

	public static final String REGEX = System.getProperty("be.nabu.artifact.resolver", "^([^.]+)\\..*");
	public static final Boolean STRICT = Boolean.parseBoolean(System.getProperty("be.nabu.artifact.resolver.strict", "true"));
	private Class<T> clazz;
	private Repository repository;
	
	public RepositoryArtifactResolver(Repository repository, Class<T> clazz) {
		this.repository = repository;
		this.clazz = clazz;
	}
	
	@SuppressWarnings("unchecked")
	public T getResolvedArtifact(String forId) {
		String resolvedId = getResolvedId(forId);
		return resolvedId == null ? null : (T) repository.resolve(resolvedId);
	}
	
	public String getResolvedId(String forId) {
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime != null) {
			List<T> artifacts = repository.getArtifacts(clazz);
			return getResolvedId(forId, artifacts);
		}
		return null;
	}

	public String getResolvedId(String forId, List<T> artifacts) {
		// let's check if an artifact has been explicitly configured for this artifact
		String longest = getContextualFor(forId, artifacts);
		if (longest != null) {
			return longest;
		}
		
		List<T> hits = null;
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		// check service-context
		if (runtime != null) {
			// we did not find for the explicit artifact, lets try the service context
			String context = ServiceUtils.getServiceContext(runtime);
			// let's check if a pool has been explicitly configured for this service context
			longest = getContextualFor(context, artifacts);
			if (longest != null) {
				return longest;
			}
			// get related hits for the service context
			hits = getRelatedFor(context, artifacts);
		}
		// if we have no service context matches, check related ids for the artifact
		if (hits == null || hits.isEmpty()) {
			hits = getRelatedFor(forId, artifacts);
		}

		// if we have exactly one hit, return that
		if (hits.size() == 1) {
			return hits.get(0).getId();
		}
		// if we have multiple, do a match based on ids, the closest one wins
		else if (hits.size() > 1) {
			T closest = null;
			int closestMatch = 0;
			
			String[] partsToMatch = forId.split("\\.");
			for (T hit : hits) {
				String[] parts = hit.getId().split("\\.");
				int matchRate = 0;
				for (int i = 0; i < Math.min(partsToMatch.length, parts.length); i++) {
					if (partsToMatch[i].equals(parts[i])) {
						matchRate++;
					}
					else {
						break;
					}
				}
				if (matchRate > closestMatch) {
					closest = hit;
				}
			}
			return closest == null ? null : closest.getId();
		}
		return null;
	}
	
	private String getContextualFor(String forId, List<T> artifacts) {
		String longest = null;
		String longestContext = null;
		for (T artifact : artifacts) {
			String artifactContext = artifact instanceof ContextualArtifact ? ((ContextualArtifact) artifact).getContext() : artifact.getId().replaceAll(REGEX, "$1");
			if (artifactContext != null) {
				for (String context : artifactContext.split("[\\s]*,[\\s]*")) {
					if (forId.equals(context) || forId.startsWith(context + ".")) {
						if (longestContext == null || context.length() > longestContext.length()) {
							longestContext = context;
							longest = artifact.getId();
						}
					}
				}
			}
		}
		return longest;
	}
	
	private List<T> getRelatedFor(String forId, List<T> artifacts) {
		String longest = null;
		List<T> matches = new ArrayList<T>();
		for (T artifact : artifacts) {
			String id = artifact.getId();
			while (id != null) {
				if (forId.startsWith(id)) {
					if (longest == null || id.length() > longest.length()) {
						longest = id;
						matches.add(artifact);
					}
				}
				int index = id.lastIndexOf('.');
				id = index <= 0 ? null : id.substring(0, index);
			}
		}
		return matches;
	}

}

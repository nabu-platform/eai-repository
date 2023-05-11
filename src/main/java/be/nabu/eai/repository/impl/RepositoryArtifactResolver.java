package be.nabu.eai.repository.impl;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ContextualArtifact;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;

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
		List<T> artifacts = repository.getArtifacts(clazz);
		return getResolvedId(forId, artifacts);
	}

	public String getResolvedId(String forId, List<T> artifacts) {
		// let's check if an artifact has been explicitly configured for this artifact (or its parents)
		String longest = getContextualFor(forId, artifacts);
		if (longest != null) {
			return longest;
		}
		
		List<T> hits = null;
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		// check service-context
		if (runtime != null) {
			// capture the context already
			String context = ServiceUtils.getServiceContext(runtime);
			
			// @2023-03-15: we want to move more towards business packages that encapsulate business data. this means just checking the service itself and the overarching context is NOT enough
			// we need to check all the intermediate services _first_ to see if a connection was defined for them.
			// this does mean we can't have "stray" connections on intermediate services
			// we only check for contextual, related matches are soft deprecated from now on because we (almost?) never use them
			while (runtime != null) {
				Service unwrapped = ServiceUtils.unwrap(runtime.getService());
				if (unwrapped instanceof DefinedService) {
					longest = getContextualFor(((DefinedService) unwrapped).getId(), artifacts);
					if (longest != null) {
						return longest;
					}
				}
				runtime = runtime.getParent();
			}
			
			
			// we did not find for the explicit artifact, lets try the service context
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
		// we also check system properties to see if there is a longer match
		// you can configure for example "context.nabu=project.shared.jdbc,project.providers.impl"
		// at that point, we will pick up the correct part
		String idToCheck = forId;
		while (idToCheck != null && (longestContext == null || idToCheck.length() > longestContext.length())) {
			String property = System.getProperty("context." + idToCheck);
			if (property != null) {
				for (String part : property.split("[\\s]*,[\\s]*")) {
					Artifact resolve = repository.resolve(part);
					if (resolve != null && clazz.isAssignableFrom(resolve.getClass())) {
						longest = resolve.getId();
						longestContext = idToCheck;
						// we won't find a longer one in this particular case...
						break;
					}
				}
			}
			int index = idToCheck.lastIndexOf('.');
			idToCheck = index <= 0 ? null : idToCheck.substring(0, index);
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

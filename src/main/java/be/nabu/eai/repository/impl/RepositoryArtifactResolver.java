/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.repository.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ContextualArtifact;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;

public class RepositoryArtifactResolver<T extends Artifact> {

	/**
	 * There are at least two strategies for resolving connections depending on the usecase at hand.
	 * 
	 * We want to have "business" packages (and utility packages) that have their own connections and can be used by other higher level packages.
	 * 
	 * This means our service stack might look like this:
	 * 
	 * higherPackageRootService -> businessPackageUtilityService -> nabuFrameworkService
	 * 
	 * It should always be possible to set a contextual intercept/override (whatever you want to call it) on the framework service for instance dictating that the system should use a particular connection for anything in that namespace
	 * 
	 * But assuming you haven't done that, there are two ways to look at this:
	 * 
	 * 1) resolve the root (the higher package) because we assume all relevant data is there (this was the old way of doing things)
	 * The problem here is that this means our intermediate business package can never maintain its own data. Instead it should get a choice in whether or not it wants to store anything. This leads to option 2.
	 * 
	 * 2) resolve every service from the caller up to see if there is a valid connection, allowing data to be encapsulated at a certain level
	 * 
	 * By default at runtime, when you are dynamically asking the system to resolve a connection, the second strategy will be used (hierarchy).
	 * 
	 * However, suppose you have frameworks like the process engine, logging, signals... frameworks that intercept existing logic to log the results somewhere (and perhaps act on them).
	 * The rules for these frameworks will mostly reside in the higher business package because it is (usually) from these end results that we coordinate the logic, those frameworks might op for the root strategy, skippîng the intermediate
	 * 
	 * However that does mean that intermediate packages can never really use the process engine...(?)
	 */
	public enum Strategy {
		// we scan the hierarchy of services to find the closest match to wherever we are running
		// the root service context is the _last_ one that is checked
		HIERARCHY,
		// we scan for a contextual match for the artifact directly and if not found, we fall back to the root rather than checking everything in between
		ROOT
	}
	
	public static final String REGEX = System.getProperty("be.nabu.artifact.resolver", "^([^.]+)\\..*");
	public static final Boolean STRICT = Boolean.parseBoolean(System.getProperty("be.nabu.artifact.resolver.strict", "true"));
	private Class<T> clazz;
	private Repository repository;
	private Strategy strategy = Strategy.HIERARCHY;
	private List<String> requiredDependencies;
	
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
			if (checkAvailableDependencies(longest)) {
				return longest;
			}
		}
		
		List<T> hits = null;
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		// check service-context
		if (runtime != null) {
			// capture the context already
			String context = ServiceUtils.getServiceContext(runtime);
			
			if (strategy == Strategy.HIERARCHY) {
				// @2023-03-15: we want to move more towards business packages that encapsulate business data. this means just checking the service itself and the overarching context is NOT enough
				// we need to check all the intermediate services _first_ to see if a connection was defined for them.
				// this does mean we can't have "stray" connections on intermediate services
				// we only check for contextual, related matches are soft deprecated from now on because we (almost?) never use them
				while (runtime != null) {
					Service unwrapped = ServiceUtils.unwrap(runtime.getService());
					if (unwrapped instanceof DefinedService) {
						longest = getContextualFor(((DefinedService) unwrapped).getId(), artifacts);
						if (longest != null) {
							if (checkAvailableDependencies(longest)) {
								return longest;
							}
						}
					}
					runtime = runtime.getParent();
				}
			}
			
			// we did not find for the explicit artifact, lets try the service context
			// let's check if a pool has been explicitly configured for this service context
			longest = getContextualFor(context, artifacts);
			if (longest != null) {
				if (checkAvailableDependencies(longest)) {
					return longest;
				}
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
			if (checkAvailableDependencies(hits.get(0).getId())) {
				return hits.get(0).getId();
			}
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
			if (closest != null && checkAvailableDependencies(closest.getId())) {
				return closest.getId();
			}
		}
		return null;
	}

	private boolean checkAvailableDependencies(String longest) {
		boolean dependenciesAvailable = true;
		// if we have required dependencies, validate that they are present
		if (requiredDependencies != null && !requiredDependencies.isEmpty()) {
			List<String> dependencies = repository.getReferences(longest);
			if (dependencies == null || dependencies.isEmpty()) {
				dependenciesAvailable = false;
			}
			if (dependenciesAvailable) {
				for (String requiredDependency : requiredDependencies) {
					if (dependencies.indexOf(requiredDependency) < 0) {
						dependenciesAvailable = false;
						break;
					}
				}
			}
		}
		return dependenciesAvailable;
	}
	
	public static Object getContextualFor(String forId, List<Object> available, String field) {
		Object result = null;
		if (forId != null) {
			result = getContextualObjectFor(forId, available, field);
		}
		if (result == null) {
			ServiceRuntime runtime = ServiceRuntime.getRuntime();
			String context = runtime == null ? null : ServiceUtils.getServiceContext(runtime);
			while (runtime != null && result == null) {
				Service unwrapped = ServiceUtils.unwrap(runtime.getService());
				if (unwrapped instanceof DefinedService) {
					result = getContextualObjectFor(((DefinedService) unwrapped).getId(), available, field);
				}
				runtime = runtime.getParent();
			}
			if (result == null && context != null) {
				result = getContextualObjectFor(context, available, field);
			}
		}
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object getContextualObjectFor(String forId, List<Object> available, String field) {
		Object longest = null;
		String longestContext = null;
		Object nullMatch = null;
		for (Object object : available) {
			if (object == null) {
				continue;
			}
			if (!(object instanceof ComplexContent)) {
				object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
				if (object == null) {
					continue;
				}
			}
			Object contextValue = ((ComplexContent) object).get(field == null ? "context" : field);
			if (contextValue == null) {
				if (nullMatch == null) {
					nullMatch = object;
				}
				continue;
			}
			else if (!(contextValue instanceof Iterable)) {
				contextValue = Arrays.asList(contextValue.toString().split("[\\s]*,[\\s]*"));
			}
			for (Object single : (Iterable) contextValue) {
				if (single == null) {
					if (nullMatch == null) {
						nullMatch = object;
					}
					continue;
				}
				String context = single.toString();
				if (forId.equals(context) || forId.startsWith(context + ".")) {
					if (longestContext == null || context.length() > longestContext.length()) {
						longestContext = context;
						longest = object;
					}
				}
			}
		}
		return longest == null ? nullMatch : longest;
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

	public Strategy getStrategy() {
		return strategy;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}

	public List<String> getRequiredDependencies() {
		return requiredDependencies;
	}

	public void setRequiredDependencies(List<String> requiredDependencies) {
		this.requiredDependencies = requiredDependencies;
	}

}

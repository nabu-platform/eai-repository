package be.nabu.eai.repository.api;

import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;

public interface ReviewableArtifactFragmentManager<T extends Artifact> extends ArtifactFragmentManager<T> {
	public interface ReviewResource {
		/**
		 * The name of the resource, for instance "review.html"
		 */
		public String getName();
		/**
		 * The content type to expose for this resource.
		 */
		public String getContentType();
		/**
		 * The actual content.
		 */
		public byte [] getContent();
	}
	/**
	 * Returns the static review resource to use per logical fragment type.
	 * The key matches ArtifactFragment.getFragmentType().
	 */
	public Map<String, ReviewResource> getReviewResources();
}

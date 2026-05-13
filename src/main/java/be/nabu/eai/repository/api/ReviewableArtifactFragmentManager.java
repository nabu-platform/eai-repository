package be.nabu.eai.repository.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface ReviewableArtifactFragmentManager<T extends Artifact> extends ArtifactFragmentManager<T> {
	public interface ReviewResource {
		/**
		 * The name of the resource, for instance "review.html"
		 */
		public String getName();
		/**
		 * The default resource is the main resource sent back via review 
		 */
		public boolean isDefault();
		/**
		 * Static resources can be stored for all reviews and must not be stored per review 
		 */
		public boolean isStatic();
		/**
		 * The actual content
		 */
		public byte [] getContent();
	}
	public List<ReviewResource> reviewFragmentUpdate(T artifact, String path, String oldContent, String newContent);
}

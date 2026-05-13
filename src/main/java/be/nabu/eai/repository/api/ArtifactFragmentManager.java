package be.nabu.eai.repository.api;

import java.util.List;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.validator.api.Validation;

public interface ArtifactFragmentManager<T extends Artifact> {
	public interface ArtifactFragment {
		/**
		 * Can the fragment be edited?
		 */
		public boolean isEditable();
		/**
		 * Can the fragment be removed?
		 */
		public boolean isRemovable();
		/**
		 * The path of the fragment within the artifact
		 */
		public String getPath();
		/**
		 * The content, we will try our best to transform this to JSON to fit within the structured content
		 */
		public String getContent();
		/**
		 * The content type of what is returned, mostly application/xml
		 */
		public String getContentType();
		/**
		 * The artifact it belongs to
		 */
		public String getArtifactId();
		/**
		 * The type of artifact it belongs to, this may impact what should go into the fragment
		 * Need a stable slug for this, based it on the "name" (prettified), but ideally provide a stable slug
		 */
		public String getArtifactType();
		/**
		 * Structural metadata for this fragment, for example it might contain super types for references for data types for easily resolving the extension hierarchy
		 */
		public Map<String, String> getProperties();
	}
	public List<ArtifactFragment> listFragments(T artifact);
	public List<Validation<?>> updateFragment(T artifact, String path, String content);
	public List<Validation<?>> deleteFragment(T artifact, String path);
	public List<Validation<?>> createFragment(T artifact, String path, String content);
	/**
	 * Explain how these fragments can be used.
	 */
	public String getGuidelines();
	
	public Class<T> getArtifactClass();
	
}

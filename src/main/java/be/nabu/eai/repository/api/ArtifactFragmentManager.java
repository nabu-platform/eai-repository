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
		 * The logical fragment type within the artifact, for example metadata, structure, pipeline or service.
		 * This is intended for fragment-specific guidance lookup.
		 */
		public String getFragmentType();
		/**
		 * Structural metadata for this fragment, for example it might contain super types or references for easier downstream resolution.
		 */
		public Map<String, String> getProperties();
	}
	/**
	 * The logical type of artifact managed by this manager, used for manager and guideline resolution.
	 * Need a stable slug for this, based on the "name" (prettified), but ideally provide a stable slug.
	 */
	public String getArtifactType();
	public String getArtifactCategory();
	public List<ArtifactFragment> listFragments(T artifact);
	public List<Validation<?>> updateFragment(T artifact, String path, String oldContent, String newContent);
	public List<Validation<?>> deleteFragment(T artifact, String path);
	public List<Validation<?>> createFragment(T artifact, String path, String initialContent);
	
	/**
	 * Explain how these fragments can be used.
	 * If fragment types are provided, only return the relevant subset of the guidance.
	 */
	public default String getGuidelines(List<String> fragmentTypes) { return null; }
	
	public Class<T> getArtifactClass();
	
}

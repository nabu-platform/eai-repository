package be.nabu.eai.repository.impl;

import java.util.Map;

import be.nabu.eai.repository.api.ArtifactFragmentManager.ArtifactFragment;

public class EditableAliasArtifactFragment implements ArtifactFragment {

	private ArtifactFragment delegate;
	private boolean editable;

	public EditableAliasArtifactFragment(ArtifactFragment delegate, boolean editable) {
		this.delegate = delegate;
		this.editable = editable;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public boolean isRemovable() {
		return delegate.isRemovable();
	}

	@Override
	public String getPath() {
		return delegate.getPath();
	}

	@Override
	public String getContent() {
		return delegate.getContent();
	}

	@Override
	public String getContentType() {
		return delegate.getContentType();
	}

	@Override
	public String getArtifactId() {
		return delegate.getArtifactId();
	}

	@Override
	public String getFragmentType() {
		return delegate.getFragmentType();
	}

	@Override
	public Map<String, String> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public Long getLastModified() {
		return delegate.getLastModified();
	}
}

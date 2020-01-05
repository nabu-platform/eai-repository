package be.nabu.eai.repository.events;

public class ResourceEvent {
	public enum ResourceState {
		CREATE,
		DELETE,
		UPDATE
	}
	
	private ResourceState state;
	private String artifactId, path;
	
	public ResourceState getState() {
		return state;
	}
	public void setState(ResourceState state) {
		this.state = state;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
}

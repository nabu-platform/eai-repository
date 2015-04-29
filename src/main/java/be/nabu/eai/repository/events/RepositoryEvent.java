package be.nabu.eai.repository.events;

public class RepositoryEvent {

	public enum RepositoryState {
		LOAD,
		UNLOAD,
		RELOAD
	}
	
	private boolean done;
	private RepositoryState state;
	
	public RepositoryEvent(RepositoryState state, boolean done) {
		this.state = state;
		this.done = done;
	}
	
	public boolean isDone() {
		return done;
	}
	public RepositoryState getState() {
		return state;
	}
	
}

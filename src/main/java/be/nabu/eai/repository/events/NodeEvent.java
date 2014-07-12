package be.nabu.eai.repository.events;

public class NodeEvent {
	public enum State {
		LOAD,
		SAVE,
		CREATE,
		EXECUTE,
		DELETE
	}
	
	private State state;
	private String id;
	private boolean done;
	
	public NodeEvent(String id, State state, boolean done) {
		this.id = id;
		this.state = state;
		this.done = done;
	}

	public State getState() {
		return state;
	}
	public String getId() {
		return id;
	}
	public boolean isDone() {
		return done;
	}
}

package be.nabu.eai.repository.events;

import be.nabu.eai.repository.api.Node;

public class NodeEvent {
	public enum State {
		LOAD,
		UNLOAD,
		SAVE,
		CREATE,
		EXECUTE,
		DELETE,
		RELOAD
	}
	
	private State state;
	private String id;
	private boolean done;
	private Node node;
	
	public NodeEvent(String id, Node node, State state, boolean done) {
		this.id = id;
		this.node = node;
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

	public Node getNode() {
		return node;
	}
	
	@Override
	public String toString() {
		return id + " - " + state + " (" + done + ")";
	}
}

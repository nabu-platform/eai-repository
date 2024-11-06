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

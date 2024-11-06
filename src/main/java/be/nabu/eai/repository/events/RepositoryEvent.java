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

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

package be.nabu.eai.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.services.SimpleTransactionContext;

public class EAITransactionContext extends SimpleTransactionContext {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void commit(String id) {
		logger.debug("Committing transaction: " + id);
		super.commit(id);
	}

	@Override
	public void rollback(String id) {
		logger.warn("Rolling back transaction: " + id);
		super.rollback(id);
	}

	@Override
	public String start() {
		String id = super.start();
		logger.debug("Starting transaction: " + id);
		return id;
	}
}

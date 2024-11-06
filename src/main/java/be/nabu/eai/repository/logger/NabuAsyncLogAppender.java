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

package be.nabu.eai.repository.logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.services.api.DefinedService;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AsyncAppenderBase;

public class NabuAsyncLogAppender extends AsyncAppenderBase<ILoggingEvent> {

	private DefinedService service;
	private Repository repository;
	private List<String> stackOptionList = Arrays.asList("full");
	private Map<String, String> properties;
	
	public NabuAsyncLogAppender(Repository repository, DefinedService service, Map<String, String> properties) {
		this.repository = repository;
		this.service = service;
		this.properties = properties;
	}
	
	@Override
	protected void append(ILoggingEvent event) {
		NabuLogAppender.log(event, repository, service, stackOptionList, properties);
	}

}

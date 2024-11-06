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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;

public class NabuLogAppender extends AppenderBase<ILoggingEvent> {

	private DefinedService service;
	private Repository repository;
	private List<String> stackOptionList = Arrays.asList("full");
	private Map<String, String> properties;
	
	public NabuLogAppender(Repository repository, DefinedService service, Map<String, String> properties) {
		this.repository = repository;
		this.service = service;
		this.properties = properties;
	}
	
	@Override
	protected void append(ILoggingEvent event) {
		log(event, repository, service, stackOptionList, properties);
	}

	public static void log(ILoggingEvent event, Repository repository, DefinedService service, List<String> stackOptionList, Map<String, String> properties) {
		NabuLogMessage message = toMessage(event, repository, stackOptionList);
		
		ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
		if (properties != null) {
			for (String key : properties.keySet()) {
				input.set(key.replace(".", "/"), properties.get(key));
			}
		}
		input.set("log", message);
		repository.getServiceRunner().run(service, repository.newExecutionContext(SystemPrincipal.ROOT), input);
	}

	public static NabuLogMessage toMessage(ILoggingEvent event, Repository repository, List<String> stackOptionList) {
		NabuLogMessage message = new NabuLogMessage();
		message.setName(repository.getName());
		message.setGroup(repository.getGroup());
		if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
			message.setSeverity(Severity.ERROR);
		}
		else if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
			message.setSeverity(Severity.WARNING);
		}
		else if (event.getLevel().isGreaterOrEqual(Level.INFO)) {
			message.setSeverity(Severity.INFO);
		}
		
		message.setTimestamp(new Date(event.getTimeStamp()));
		message.setContext(new ArrayList<String>(Arrays.asList(event.getLoggerName())));
		message.setMessage(event.getFormattedMessage());
		
		IThrowableProxy throwable = event.getThrowableProxy();
		if (throwable != null) {
			ThrowableProxyConverter converter = new ThrowableProxyConverter();
			converter.setOptionList(stackOptionList);
			converter.start();
			message.setDescription(converter.convert(event));
		}
		return message;
	}

}

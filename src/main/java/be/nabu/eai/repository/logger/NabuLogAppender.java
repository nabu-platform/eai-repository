package be.nabu.eai.repository.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
	
	public NabuLogAppender(Repository repository, DefinedService service) {
		this.repository = repository;
		this.service = service;
	}
	
	@Override
	protected void append(ILoggingEvent event) {
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
		
		ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
		input.set("log", message);
		repository.getServiceRunner().run(service, repository.newExecutionContext(SystemPrincipal.ROOT), input);
	}

}
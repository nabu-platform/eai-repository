package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeUtils;

public class CorrelationIdEnricher implements EventEnricher {
	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		String value = getCorrelationIdAnywhere();
		if (value != null) {
			if (!(object instanceof ComplexContent)) {
				object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
			}
			if (object != null) {
				// if we have a field called "sessionId", we enrich it
				if (((ComplexContent) object).getType().get("correlationId") != null) {
					Object current = ((ComplexContent) object).get("correlationId");
					if (current == null) {
						((ComplexContent) object).set("correlationId", value);
					}
				}
			}
		}
		return null;
	}
	
	private static String getCorrelationIdAnywhere() {
		String value = null;
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime != null) {
			value = runtime.getCorrelationId();
		}
		if (value == null) {
			value = getCorrelationId();
		}
		return value;
	}
	
	public static String getCorrelationId() {
		Object currentRequest = RequestProcessor.getCurrentRequest();
		if (currentRequest instanceof HTTPRequest) {
			ModifiablePart content = ((HTTPRequest) currentRequest).getContent();
			if (content != null) {
				Header header = MimeUtils.getHeader("X-Correlation-Id", content.getHeaders());
				if (header != null) {
					return header.getValue();
				}
			}
		}
		return null;
	}
}

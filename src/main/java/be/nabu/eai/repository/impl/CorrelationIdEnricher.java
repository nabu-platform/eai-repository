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

package be.nabu.eai.repository.impl;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.core.ServerHeader;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeUtils;

public class CorrelationIdEnricher implements EventEnricher {
	private static final String TRACEPARENT_HEADER = "traceparent";
	private static final int TRACE_ID_LENGTH = 32;
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
				String conversationId = getConversationId();
				if (conversationId != null) {
					if (((ComplexContent) object).getType().get("conversationId") != null) {
						Object current = ((ComplexContent) object).get("conversationId");
						if (current == null) {
							((ComplexContent) object).set("conversationId", conversationId);
						}
					}	
				}
				String narrativeId = getNarrativeId();
				if (narrativeId != null) {
					if (((ComplexContent) object).getType().get("narrativeId") != null) {
						Object current = ((ComplexContent) object).get("narrativeId");
						if (current == null) {
							((ComplexContent) object).set("narrativeId", narrativeId);
						}
					}	
				}
			}
		}
		return null;
	}
	
	private static String getNarrativeId() {
		String value = null;
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime != null) {
			value = runtime.getNarrativeId();
		}
		return value;
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
				Header header = MimeUtils.getHeader(ServerHeader.NAME_CORRELATION_ID, content.getHeaders());
				if (header != null) {
					String value = header.getValue();
					return value;
				}
				Header traceHeader = MimeUtils.getHeader(TRACEPARENT_HEADER, content.getHeaders());
				if (traceHeader != null) {
					String traceId = extractTraceIdFromTraceParent(traceHeader.getValue());
					if (traceId != null) {
						return traceId;
					}
				}
			}
		}
		return null;
	}

	public static String getOtelTraceId() {
		try {
			Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
			Object span = spanClass.getMethod("current").invoke(null);
			if (span == null) {
				return null;
			}
			Object spanContext = spanClass.getMethod("getSpanContext").invoke(span);
			if (spanContext == null) {
				return null;
			}
			Class<?> spanContextClass = Class.forName("io.opentelemetry.api.trace.SpanContext");
			Object traceIdValue = spanContextClass.getMethod("getTraceId").invoke(spanContext);
			if (traceIdValue instanceof String) {
				String traceId = ((String) traceIdValue).trim();
				return isValidTraceId(traceId) ? traceId : null;
			}
		}
		catch (ClassNotFoundException e) {
			return null;
		}
		catch (Exception e) {
			return null;
		}
		return null;
	}
	
	public static String getConversationId() {
		Object currentRequest = RequestProcessor.getCurrentRequest();
		if (currentRequest instanceof HTTPRequest) {
			ModifiablePart content = ((HTTPRequest) currentRequest).getContent();
			if (content != null) {
				Header header = MimeUtils.getHeader(ServerHeader.NAME_CONVERSATION_ID, content.getHeaders());
				if (header != null && !header.getValue().trim().isEmpty()) {
					return header.getValue().trim();
				}
			}
		}
		return null;
	}

	private static String extractTraceIdFromTraceParent(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		String[] parts = trimmed.split("-", -1);
		if (parts.length < 4) {
			return null;
		}
		String traceId = parts[1];
		return isValidTraceId(traceId) ? traceId : null;
	}

	private static boolean isValidTraceId(String traceId) {
		if (traceId == null) {
			return false;
		}
		String value = traceId.trim();
		if (value.length() != TRACE_ID_LENGTH) {
			return false;
		}
		boolean allZeros = true;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			int digit = Character.digit(ch, 16);
			if (digit < 0) {
				return false;
			}
			if (digit != 0) {
				allZeros = false;
			}
		}
		return !allZeros;
	}
}

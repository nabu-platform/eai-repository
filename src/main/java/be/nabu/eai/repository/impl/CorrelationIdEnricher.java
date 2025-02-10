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
				Header header = MimeUtils.getHeader(ServerHeader.NAME_CORRELATION_ID, content.getHeaders());
				if (header != null) {
					String value = header.getValue();
					// the http processor already prepends this?
//					String conversationId = getConversationId();
//					if (conversationId != null) {
//						value = conversationId + ":" + value;
//					}
					return value;
				}
			}
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
}

package be.nabu.eai.repository.artifacts.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPExceptionFormatter;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class RepositoryExceptionFormatter implements HTTPExceptionFormatter {

	private Map<Integer, String> errorTemplates = new HashMap<Integer, String>();
	private String defaultErrorTemplate = "<html><head><title>${code}: ${message}</title></head><body><h1>${code}: ${message}</h1><pre>${description}</pre></body></html>";
	
	private List<String> whitelistedCodes = new ArrayList<String>();
	private Map<String, List<String>> artifactCodes = new HashMap<String, List<String>>();
	
	@Override
	public HTTPResponse format(HTTPRequest request, HTTPException exception) {
		List<String> requestedTypes = new ArrayList<String>();
		List<String> requestedCharsets = new ArrayList<String>();
		if (request != null && request.getContent() != null) {
			requestedTypes.addAll(MimeUtils.getAcceptedContentTypes(request.getContent().getHeaders()));
			requestedCharsets.addAll(MimeUtils.getAcceptedCharsets(request.getContent().getHeaders()));
		}
		Charset charset = Charset.defaultCharset();
		if (!requestedCharsets.isEmpty()) {
			try {
				charset = Charset.forName(requestedCharsets.get(0));
			}
			catch (Exception e) {
				// ignore
			}
		}
		ServiceException serviceException = getServiceException(exception);
		StructuredResponse response = new StructuredResponse();
		// we have a service exception that can be reported
		if (serviceException != null && whitelistedCodes.contains(serviceException.getCode())) {
			response.setCode(serviceException.getCode());
			response.setMessage(serviceException.getPlainMessage());
			if (EAIResourceRepository.isDevelopment()) {
				response.setDescription(serviceException.getServiceStack() + "\n\n" + stacktrace(exception));
			}
		}
		else {
			exception = getHTTPException(exception);
			response.setCode("HTTP-" + exception.getCode());
			response.setMessage(HTTPCodes.getMessage(exception.getCode()));
			if (EAIResourceRepository.isDevelopment()) {
				response.setDescription(stacktrace(exception));
			}
		}
		MarshallableBinding binding = null;
		String contentType = "text/html";
		ComplexType resolved = (ComplexType) BeanResolver.getInstance().resolve(StructuredResponse.class);
		// a default browser will likely send both a request for HTML and XML
		// we want the HTML view for anyone approaching through the browser
		// if you specifically want XML, don't include HTML in the list
		if (!requestedTypes.contains("text/html")) {
			if (requestedTypes.contains("application/xml")) {
				binding = new XMLBinding(resolved, charset);
				contentType = "application/xml";
			}
			else if (requestedTypes.contains("application/json")) {
				binding = new JSONBinding(resolved, charset);
				contentType = "application/json";
			}
		}
		if (binding == null) {
			binding = new TemplateMarshallableBinding(errorTemplates.containsKey(exception.getCode()) ? errorTemplates.get(exception.getCode()) : defaultErrorTemplate, charset);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			binding.marshal(output, new BeanInstance<StructuredResponse>(response));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte [] bytes = output.toByteArray();
		return new ExceptionHTTPResponse(exception.getCode(), HTTPCodes.getMessage(exception.getCode()), new PlainMimeContentPart(null, IOUtils.wrap(bytes, true), 
			new MimeHeader("Connection", "close"),
			new MimeHeader("Content-Length", "" + bytes.length),
			new MimeHeader("Content-Type", contentType + "; charset=" + charset.displayName())
		), exception, response);
	}

	private String stacktrace(HTTPException exception) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printer = new PrintWriter(stringWriter);
		exception.printStackTrace(printer);
		printer.flush();
		return stringWriter.toString();
	}
	
	private ServiceException getServiceException(Throwable throwable) {
		while(throwable != null) {
			if (throwable instanceof ServiceException) {
				return (ServiceException) throwable;
			}
			else {
				throwable = throwable.getCause();
			}
		}
		return null;
	}
	
	private HTTPException getHTTPException(Throwable throwable) {
		HTTPException deepest = null;
		while (throwable != null) {
			if (throwable instanceof HTTPException) {
				deepest = (HTTPException) throwable;
			}
			throwable = throwable.getCause();
		}
		return deepest;
	}
	
	public void register(String artifact, Collection<String> codes) {
		if (artifactCodes.containsKey(artifact)) {
			whitelistedCodes.removeAll(artifactCodes.get(artifact));
			artifactCodes.get(artifact).clear();
		}
		else {
			artifactCodes.put(artifact, new ArrayList<String>());
		}
		if (codes != null) {
			artifactCodes.get(artifact).addAll(codes);
			whitelistedCodes.addAll(codes);
		}
	}
	
	public void unregister(String artifact) {
		if (artifactCodes.containsKey(artifact)) {
			whitelistedCodes.removeAll(artifactCodes.get(artifact));
			artifactCodes.remove(artifact);
		}
	}
	
	@XmlRootElement(name = "exception")
	public static class StructuredResponse {
		private String code;
		private String message;
		private String description;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
	}
	
	public static class ExceptionHTTPResponse extends DefaultHTTPResponse {
		
		private StructuredResponse structured;
		private HTTPException exception;

		public ExceptionHTTPResponse(int code, String message, ModifiablePart content, HTTPException exception, StructuredResponse response) {
			super(code, message, content);
			this.exception = exception;
			structured = response;
		}
		
		public StructuredResponse getStructured() {
			return structured;
		}
		public HTTPException getException() {
			return exception;
		}
	}
	
	public static class TemplateMarshallableBinding implements MarshallableBinding {

		private String template;
		private Charset charset;
		
		public TemplateMarshallableBinding(String template, Charset charset) {
			this.template = template;
			this.charset = charset;
		}
		
		@Override
		public void marshal(OutputStream arg0, ComplexContent arg1, Value<?>... arg2) throws IOException {
			String content = template;
			for (Element<?> child : TypeUtils.getAllChildren(arg1.getType())) {
				Object object = arg1.get(child.getName());
				content = content.replace("${" + child.getName() + "}", object == null ? "" : object.toString());
			}
			arg0.write(content.getBytes(charset));
		}
	}
}

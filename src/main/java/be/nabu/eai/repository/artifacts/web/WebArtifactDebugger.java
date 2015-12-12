package be.nabu.eai.repository.artifacts.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.impl.ResponseMethods;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.services.vm.api.Step;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeUtils;

public class WebArtifactDebugger {

	private SessionProvider sessionProvider;
	private static ThreadLocal<String> sessionId = new ThreadLocal<String>();
	private static ThreadLocal<DebugServiceTracker> debug = new ThreadLocal<DebugServiceTracker>();
	private String serverPath;
	private ResponseListener responseListener;
	private RequestListener requestListener;
	public static final String SESSION_FIELD = "$debug";
	private DebugServiceTracker serviceTracker;

	public WebArtifactDebugger(String serverPath, SessionProvider sessionProvider) {
		this.serverPath = serverPath;
		this.sessionProvider = sessionProvider;
		this.requestListener = new RequestListener();
		this.responseListener = new ResponseListener();
	}

	public static interface DebugStep {
		public Date getStarted();
		public Date getStopped();
		public List<DebugStep> getSteps();
		public List<Object> getReports();
		public Exception getException();
		public void stop(Exception exception);
	}
	
	@XmlRootElement(name = "request")
	public static class DebugRequest implements DebugStep {
		private Header [] requestHeaders, responseHeaders;
		private String method, target;
		private Date started, stopped;
		private int responseCode;
		private String responseMessage;
		private Double requestVersion, responseVersion;
		private List<DebugStep> steps = new ArrayList<DebugStep>();
		private Exception exception;
		private List<Object> reports = new ArrayList<Object>();
		public DebugRequest(HTTPRequest request) {
			this.method = request.getMethod();
			this.target = request.getTarget();
			this.requestVersion = request.getVersion();
			if (request.getContent() != null) {
				this.requestHeaders = request.getContent().getHeaders();
			}
			this.started = new Date();
		}
		public Header[] getRequestHeaders() {
			return requestHeaders;
		}
		public Header[] getResponseHeaders() {
			return responseHeaders;
		}
		public String getMethod() {
			return method;
		}
		public String getTarget() {
			return target;
		}
		@Override
		public Date getStarted() {
			return started;
		}
		@Override
		public Date getStopped() {
			return stopped;
		}
		public int getResponseCode() {
			return responseCode;
		}
		public String getResponseMessage() {
			return responseMessage;
		}
		public Double getRequestVersion() {
			return requestVersion;
		}
		public Double getResponseVersion() {
			return responseVersion;
		}
		public List<DebugStep> getSteps() {
			return steps;
		}
		public Exception getException() {
			return exception;
		}
		public void setException(Exception exception) {
			this.exception = exception;
		}
		public List<Object> getReports() {
			return reports;
		}
		public void stop(HTTPResponse response) {
			this.stopped = new Date();
			this.responseCode = response.getCode();
			this.responseMessage = response.getMessage();
			this.responseVersion = response.getVersion();
			if (response.getContent() != null) {
				this.responseHeaders = response.getContent().getHeaders();
			}
		}
		@Override
		public void stop(Exception exception) {
			stopped = new Date();
			this.exception = exception;
		}
	}
	public static class DebugStepImpl implements DebugStep {
		private String serviceId;
		private Step step;
		private List<Object> reports = new ArrayList<Object>();
		private List<DebugStep> children = new ArrayList<DebugStep>();
		private Date started = new Date(), stopped;
		private Exception exception;
		public String getServiceId() {
			return serviceId;
		}
		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}
		public Step getStep() {
			return step;
		}
		public void setStep(Step step) {
			this.step = step;
		}
		public List<Object> getReports() {
			return reports;
		}
		public void setReports(List<Object> reports) {
			this.reports = reports;
		}
		public Date getStarted() {
			return started;
		}
		public void setStarted(Date started) {
			this.started = started;
		}
		public Date getStopped() {
			return stopped;
		}
		public void setStopped(Date stopped) {
			this.stopped = stopped;
		}
		public List<DebugStep> getSteps() {
			return children;
		}
		public Exception getException() {
			return exception;
		}
		public void setException(Exception exception) {
			this.exception = exception;
		}
		@Override
		public void stop(Exception exception) {
			this.stopped = new Date();
			this.exception = exception;
		}
	}
	
	public class DebugServiceTracker implements ServiceRuntimeTracker {
		
		private Stack<DebugStep> steps = new Stack<DebugStep>();
		private DebugStep root;
		
		public DebugServiceTracker(DebugStep root) {
			this.root = root;
			steps.push(root);
		}

		@Override
		public void error(Service arg0, Exception exception) {
			steps.pop().stop(exception);
		}
		@Override
		public void report(Object report) {
			steps.peek().getReports().add(report);
		}
		@Override
		public void start(Service service) {
			DebugStepImpl debugStep = new DebugStepImpl();
			debugStep.setServiceId(service instanceof DefinedService ? ((DefinedService) service).getId() : "anonymous");
			steps.peek().getSteps().add(debugStep);
			steps.push(debugStep);
		}
		@Override
		public void stop(Service arg0) {
			DebugStep pop = steps.pop();
			pop.stop(null);
		}
		@Override
		public void after(Object step) {
			if (step instanceof Step) {
				steps.pop().stop(null);
			}
		}
		@Override
		public void before(Object step) {
			if (step instanceof Step) {
				DebugStepImpl debugStep = new DebugStepImpl();
				debugStep.setStep((Step) step);
				steps.peek().getSteps().add(debugStep);
				steps.push(debugStep);
			}
		}
		@Override
		public void error(Object step, Exception exception) {
			if (step instanceof Step) {
				steps.pop().stop(exception);
			}
		}

		public DebugStep getRoot() {
			return root;
		}
		
	}
	
	@XmlRootElement(name = "listing")
	public static class Listing {
		private String serverPath;
		private List<String> paths;

		public String getServerPath() {
			return serverPath;
		}
		public void setServerPath(String serverPath) {
			this.serverPath = serverPath;
		}
		public List<String> getPaths() {
			return paths;
		}
		public void setPaths(List<String> paths) {
			this.paths = paths;
		}
	}
	
	public class RequestListener implements EventHandler<HTTPRequest, HTTPResponse> {
		
		@SuppressWarnings("unchecked")
		private HTTPResponse createResponse(HTTPRequest request, Object object) {
			ComplexContent content = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
			List<String> acceptedTypes = MimeUtils.getAcceptedContentTypes(request.getContent().getHeaders());
			acceptedTypes.retainAll(ResponseMethods.allowedTypes);
			String contentType = acceptedTypes.isEmpty() 
				? ResponseMethods.allowedTypes.get(0)
				: acceptedTypes.get(0);
			Charset charset = Charset.forName("UTF-8");
			MarshallableBinding binding = MediaType.APPLICATION_JSON.equals(contentType) 
				? new JSONBinding(content.getType(), charset)
				: new XMLBinding(content.getType(), charset);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try {
				binding.marshal(output, content);
				byte[] byteArray = output.toByteArray();
				return HTTPUtils.newResponse(request, contentType, IOUtils.wrap(byteArray, true));
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public HTTPResponse handle(HTTPRequest request) {
			// register the session id as sent by the client
			sessionId.set(GlueListener.getSessionId(HTTPUtils.getCookies(request.getContent().getHeaders())));
			try {
				URI uri = HTTPUtils.getURI(request, false);
				if (uri.getPath().matches(serverPath + "debug:.+$")) {
					String requested = uri.getPath().replaceAll(serverPath + "debug:(.+)$", "$1");
					// get a listing of all the debugs in the user session
					if (requested.equals("$list")) {
						Listing listing = new Listing();
						listing.setServerPath(serverPath);
						if (sessionId.get() != null) {
							Session session = sessionProvider.getSession(sessionId.get());
							if (session != null) {
								List<DebugStep> roots = (List<DebugStep>) session.get(SESSION_FIELD);
								if (roots != null) {
									List<String> paths = new ArrayList<String>();
									for (DebugStep root : roots) {
										if (root instanceof DebugRequest) {
											paths.add(((DebugRequest) root).getTarget());
										}
									}
									listing.setPaths(paths);
								}
							}
						}
						return createResponse(request, listing);
					}
					// remove all the debugs in the user session
					else if (requested.equals("$clear")) {
						if (sessionId.get() != null) {
							Session session = sessionProvider.getSession(sessionId.get());
							if (session != null) {
								List<DebugStep> roots = (List<DebugStep>) session.get(SESSION_FIELD);
								if (roots != null) {
									roots.clear();
								}
							}
						}
						return HTTPUtils.newEmptyResponse(request);
					}
					// it should be a path pointing to the request, if there are multiple matches most recent one wins
					else {
						if (sessionId.get() != null) {
							Session session = sessionProvider.getSession(sessionId.get());
							if (session != null) {
								List<DebugStep> roots = (List<DebugStep>) session.get(SESSION_FIELD);
								if (roots != null) {
									for (int i = roots.size() - 1; i >= 0; i--) {
										DebugStep root = roots.get(i);
										if (root instanceof DebugRequest) {
											if (requested.equals(((DebugRequest) root).getTarget())) {
												return createResponse(request, root);
											}
										}
									}
								}
							}
						}
						throw new HTTPException(404, "Can not find requested debug information: " + requested);
					}
				}
				else {
					// otherwise create a new debug target
					debug.set(new DebugServiceTracker(new DebugRequest(request)));
					WebArtifactDebugger.this.serviceTracker = debug.get();
				}
			}
			catch (FormatException e) {
				throw new HTTPException(400, e);
			}
			return null;
		}
	}
	public class ResponseListener implements EventHandler<HTTPResponse, HTTPResponse> {

		@SuppressWarnings("unchecked")
		@Override
		public HTTPResponse handle(HTTPResponse response) {
			String id = sessionId.get();
			// check if the code has created a new session for this user
			if (response.getContent() != null) {
				Header[] headers = MimeUtils.getHeaders("Set-Cookie", response.getContent().getHeaders());
				for (Header header : headers) {
					if (header.getValue().startsWith(GlueListener.SESSION_COOKIE + "=")) {
						id = header.getValue().substring(GlueListener.SESSION_COOKIE.length() + 1).trim();
					}
				}
			}
			// make sure the response listener is the one attached to this thread
			if (debug.get() != null && debug.get().equals(WebArtifactDebugger.this.serviceTracker)) {
				DebugRequest request = (DebugRequest) debug.get().getRoot();
				request.stop(response);
				if (id != null) {
					Session session = sessionProvider.getSession(id);
					if (session != null) {
						List<DebugStep> roots = (List<DebugStep>) session.get(SESSION_FIELD);
						if (roots == null) {
							roots = new ArrayList<DebugStep>();
							session.set(SESSION_FIELD, roots);
						}
						roots.add(request);
					}
				}
				debug.remove();
			}
			return null;
		}
	}
	
	public static ServiceRuntimeTracker getCurrentThreadTracker() {
		return debug.get();
	}
	
	public static class AnyThreadTracker implements ServiceRuntimeTracker {

		@Override
		public void error(Service service, Exception exception) {
			if (debug.get() != null) {
				debug.get().error(service, exception);
			}
		}
		@Override
		public void error(Object step, Exception exception) {
			if (debug.get() != null) {
				debug.get().error(step, exception);
			}			
		}
		@Override
		public void report(Object report) {
			if (debug.get() != null) {
				debug.get().report(report);
			}
		}
		@Override
		public void start(Service service) {
			if (debug.get() != null) {
				debug.get().start(service);
			}			
		}
		@Override
		public void before(Object step) {
			if (debug.get() != null) {
				debug.get().before(step);
			}			
		}
		@Override
		public void stop(Service service) {
			if (debug.get() != null) {
				debug.get().stop(service);
			}			
		}
		@Override
		public void after(Object step) {
			if (debug.get() != null) {
				debug.get().after(step);
			}			
		}
	}

	public ResponseListener getResponseListener() {
		return responseListener;
	}

	public RequestListener getRequestListener() {
		return requestListener;
	}
	
}

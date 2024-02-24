package be.nabu.eai.repository;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ExecutorServiceProvider;
import be.nabu.eai.repository.api.ExtensibleEntry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ObjectEnricher;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.ArtifactUtils;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.ArtifactWithTodo;
import be.nabu.libs.artifacts.api.Todo;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.principals.DevicePrincipal;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.resources.api.FiniteResource;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.DefinedServiceInterfaceResolverFactory;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.properties.EnricherProperty;
import be.nabu.libs.types.properties.PersisterProperty;
import be.nabu.libs.types.properties.PrimaryKeyProperty;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.cep.impl.CEPUtils;
import be.nabu.utils.cep.impl.ComplexEventImpl;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class EAIRepositoryUtils {

	private static Logger logger = LoggerFactory.getLogger(EAIRepositoryUtils.class);

	public static boolean isProject(Entry entry) {
		be.nabu.eai.repository.api.Collection collection = entry.getCollection();
		if (collection != null && "project".equals(collection.getType())) {
			return true;
		}
		// any root folder that is not nabu is currently flagged as a project (retroactive shizzle!)
		// any root folder with an explicit collection that is not a project, does not count
		else if (collection == null && entry.getParent() != null && entry.getParent().getParent() == null && !"nabu".equals(entry.getName())) {
			return true;
		}
		return false;
	}
	
	public static Entry getProject(Entry entry) {
		while (entry != null && !isProject(entry)) {
			entry = entry.getParent();
		}
		return entry;
	}
	
	private static String getCode(Throwable t) {
		String code = null;
		if (t.getCause() != null) {
			code = getCode(t.getCause());
		}
		if (code == null && t instanceof ServiceException) {
			code = ((ServiceException) t).getCode();
		}
		return code;
	}
	private static List<String> getServiceStack(Throwable t) {
		List<String> stack = null;
		if (t.getCause() != null) {
			stack = getServiceStack(t.getCause());
		}
		if (stack == null && t instanceof ServiceException) {
			stack = ((ServiceException) t).getServiceStack();
		}
		return stack;
	}
	
	public static void enrich(ComplexEventImpl event, Exception e) {
		CEPUtils.enrich(event, e);
		event.setCode(getCode(e));
		List<String> serviceStack = getServiceStack(e);
		if (serviceStack != null) {
			event.setContext(serviceStack.toString());
		}
	}
	
	public static List<String> getServiceStack() {
		List<String> serviceStack = new ArrayList<String>();
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		while (runtime != null) {
			if (runtime.getService() instanceof DefinedService) {
				serviceStack.add(((DefinedService) runtime.getService()).getId());
			}
			runtime = runtime.getParent();
		}
		return serviceStack;
	}
	
	public static ValidationMessage toValidation(Throwable error) {
		StringWriter writer = new StringWriter();
		PrintWriter printer = new PrintWriter(writer);
		error.printStackTrace(printer);
		printer.flush();
		return new ValidationMessage(Severity.ERROR, error.getMessage(), writer.toString());
	}
	
	public static void message(Repository repository, String artifactId, String category, boolean clear, Validation<?>...validations) {
		Map<String, List<Validation<?>>> messages = repository.getMessages(artifactId);
		synchronized(messages) {
			if (!messages.containsKey(category)) {
				if (!messages.containsKey(category)) {
					messages.put(category, new ArrayList<Validation<?>>());
				}
			}
			List<Validation<?>> list = messages.get(category);
			synchronized(list) {
				if (clear) {
					list.clear();
				}
				list.addAll(Arrays.asList(validations));
			}
		}
	}
	
	public static Entry getEntry(Entry entry, String id) {
		// sometimes java arrays (more specifically the byte "[B") get in there...
		ParsedPath path = ParsedPath.parse(id.replace('.', '/'));
		while (entry != null && path != null) {
			entry = entry.getChild(path.getName());
			path = path.getChildPath();
		}
		return entry;
	}
	
	public static Node getNode(Repository repository, String id) {
		Entry entry = getEntry(repository.getRoot(), id);
		return entry != null && entry.isNode() ? entry.getNode() : null;
	}
	
	public static Artifact resolve(Repository repository, String id) {
		Entry entry = getEntry(repository.getRoot(), id);
		try {
			return entry != null && entry.isNode() ? entry.getNode().getArtifact() : null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static Entry getDirectoryEntry(ResourceRepository repository, String id, boolean create) throws IOException {
		ParsedPath path = ParsedPath.parse(id.replace(".", "/"));
		Entry entry = repository.getRoot();
		while (path != null) {
			Entry child = entry.getChild(path.getName());
			if (child == null && create && entry instanceof ExtensibleEntry) {
				child = ((ExtensibleEntry) entry).createDirectory(path.getName());
			}
			if (child == null) {
				return null;
			}
			entry = child;
			path = path.getChildPath();
		}
		return entry;
	}
	
	public static void zip(ZipOutputStream output, Entry entry, EntryFilter acceptor) throws IOException {
		zipEntry(output, entry, acceptor);
	}
	
	public static void zipInto(OutputStream output, Entry entry, EntryFilter acceptor) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(output);
		try {
			zipEntry(zip, entry, acceptor);
		}
		finally {
			zip.finish();
		}
	}
	
	private static boolean zipEntry(ZipOutputStream output, Entry entry, EntryFilter acceptor) throws IOException {
		boolean zipped = false;
		if (entry instanceof ResourceEntry && entry.isNode() && (acceptor == null || acceptor.accept((ResourceEntry) entry))) {
			zipNode(output, entry.getId().replace(".", "/"), ((ResourceEntry) entry).getContainer(), ((ResourceEntry) entry).getRepository(), true);
			zipped = true;
		}
		if (!entry.isLeaf() && (acceptor == null || acceptor.recurse((ResourceEntry) entry))) {
			for (Entry child : entry) {
				if (child instanceof ResourceEntry) {
					zipped |= zipEntry(output, (ResourceEntry) child, acceptor);
				}
			}
			// if we zipped anything in the folder, let's include any internal folders as well (e.g. documentation)
			if (zipped && entry instanceof ResourceEntry && !entry.isNode()) {
				ResourceContainer<?> container = ((ResourceEntry) entry).getContainer();
				// we want files like collection.xml to get copied and folders like protected which might contain documentation
				zipNode(output, entry.getId().replace(".",  "/"), container, ((ResourceEntry) entry).getRepository(), true);
			}
		}
		return zipped;
	}
	
	public static byte[] zipSingleEntry(ResourceEntry entry) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(output);
		try {
			zipNode(zip, null, entry.getContainer(), entry.getRepository(), true);
		}
		finally {
			zip.finish();
		}
		return output.toByteArray();
	}
	
	public static byte[] zipFullEntry(ResourceEntry entry) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(output);
		try {
			zipNode(zip, null, entry.getContainer(), entry.getRepository(), false);
		}
		finally {
			zip.finish();
		}
		return output.toByteArray();
	}
	
	private static void zipNode(ZipOutputStream output, String path, ResourceContainer<?> container, ResourceRepository repository, boolean limitToInternal) throws IOException {
		for (Resource resource : container) {
			String childPath = (path == null ? "" : path + "/") + resource.getName();
			System.out.println("zipping " + childPath);
			if (resource instanceof ReadableResource) {
				ZipEntry entry = new ZipEntry(childPath);
				if (resource instanceof FiniteResource) {
					entry.setSize(((FiniteResource) resource).getSize());
				}
				if (resource instanceof TimestampedResource) {
					entry.setLastModifiedTime(FileTime.fromMillis(((TimestampedResource) resource).getLastModified().getTime()));
				}
				output.putNextEntry(entry);
				ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
				try {
					IOUtils.copyBytes(readable, IOUtils.wrap(output));
				}
				finally {
					readable.close();
				}
			}
			else if (resource instanceof ResourceContainer && (!limitToInternal || repository.isInternal((ResourceContainer<?>) resource))) {
				zipNode(output, childPath, (ResourceContainer<?>) resource, repository, false);
			}
		}
	}
	
	public static interface EntryFilter {
		public boolean accept(ResourceEntry entry);
		public boolean recurse(ResourceEntry entry);
	}
	
	public static ModifiableEntry mkdirs(RepositoryEntry root, String id, boolean includeLast) throws IOException {
		ParsedPath path = ParsedPath.parse(id.replace('.', '/'));
		// resolve a parent path
		while ((includeLast && path != null) || (!includeLast && path.getChildPath() != null)) {
			Entry entry = root.getChild(path.getName());
			if (entry == null) {
				root = root.createDirectory(path.getName());
			}
			else if (entry.isNode()) {
				throw new IllegalArgumentException("Currently not allowed to embed nodes in existing nodes");
			}
		}
		return root;
	}
	
	public static ModifiableEntry getParent(ModifiableEntry root, String id, boolean includeLast) {
		ParsedPath path = ParsedPath.parse(id.replace('.', '/'));
		// resolve a parent path
		while ((includeLast && path != null) || (!includeLast && path.getChildPath() != null)) {
			Entry entry = root.getChild(path.getName());
			// if it's null, create a new entry
			if (entry == null) {
				entry = new MemoryEntry(root.getId(), root.getRepository(), root, null, (root.getId().isEmpty() ? "" : root.getId() + ".") + path.getName(), path.getName());
				root.addChildren(entry);
			}
			else if (entry.isNode()) {
				((EAINode) entry.getNode()).setLeaf(false);
			}
			root = (ModifiableEntry) entry;
			path = path.getChildPath();
		}
		return root;
	}
	
	public static <T> List<Class<T>> getImplementationsFor(Class<T> clazz) {
		return getImplementationsFor(Thread.currentThread().getContextClassLoader(), clazz);
	}
	
	private static WeakHashMap<ClassLoader, Map<Class<?>, List<Class<?>>>> cachedClasses = new WeakHashMap<ClassLoader, Map<Class<?>, List<Class<?>>>>();
	
	public static <T> List<Class<T>> getImplementationsFor(ClassLoader classLoader, Class<T> clazz) {
		return getImplementationsFor(classLoader, clazz, false);
	}
	/**
	 * Beware that this method has quite a bit of overhead so it is advised to cache the result
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<Class<T>> getImplementationsFor(ClassLoader classLoader, Class<T> clazz, boolean force) {
		Map<Class<?>, List<Class<?>>> map = cachedClasses.get(classLoader);
		if (map == null) {
			map = new HashMap<Class<?>, List<Class<?>>>();
			cachedClasses.put(classLoader, map);
		}
		if (map.get(clazz) == null || force) {
			try {
				Enumeration<URL> resources = classLoader.getResources("META-INF/services/" + clazz.getName());
				List<Class<?>> classes = new ArrayList<Class<?>>();
				if (resources != null) {
					while (resources.hasMoreElements()) {
						URL nextElement = resources.nextElement();
						InputStream stream = nextElement.openStream();
						try {
							byte[] bytes = IOUtils.toBytes(IOUtils.wrap(stream));
							String content = new String(bytes, "UTF-8");
							for (String line : content.split("[\r\n]+")) {
								try {
									Class<?> implementation = classLoader.loadClass(line);
									classes.add(implementation);
								}
								catch (ClassNotFoundException e) {
									logger.error("Could not locate implementation class '" + line + "' for interface: " + clazz);
								}
							}
						}
						finally {
							stream.close();
						}
					}
				}
				map.put(clazz, classes);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		List list = map.get(clazz);
		return (List<Class<T>>) list;
	}
	
	public static <T extends Artifact> ArtifactManager<T> getArtifactManager(Class<T> artifactClass) {
		return getArtifactManager(Thread.currentThread().getContextClassLoader(), artifactClass);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends Artifact> ArtifactManager<T> getArtifactManager(ClassLoader loader, Class<T> artifactClass) {
		ArtifactManager<T> closest = null;
		for (Class<ArtifactManager> managerClass : getImplementationsFor(loader, ArtifactManager.class, false)) {
			try {
				ArtifactManager manager = managerClass.newInstance();
				if (manager.getArtifactClass().isAssignableFrom(artifactClass)) {
					if (closest == null || closest.getArtifactClass().isAssignableFrom(manager.getArtifactClass())) {
						closest = (ArtifactManager<T>) manager;
					}
				}
			}
			catch (Exception e) {
				logger.error("Could not load manager: " + managerClass, e);
			}
		}
		return closest;
	}
	
	public static Resource getResource(ResourceEntry entry, String name, boolean create) throws IOException {
		Resource resource = entry.getContainer().getChild(name);
		if (resource == null && create) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create(name, "application/xml");
		}
		if (resource == null) {
			throw new FileNotFoundException("Can not find " + name);
		}
		return resource;
	}
	
	public static String uncamelify(String string) {
		return NamingConvention.UNDERSCORE.apply(string, NamingConvention.LOWER_CAMEL_CASE);
//		StringBuilder builder = new StringBuilder();
//		boolean previousUpper = false;
//		for (int i = 0; i < string.length(); i++) {
//			String substring = string.substring(i, i + 1);
//			if (substring.equals(substring.toLowerCase()) || i == 0) {
//				previousUpper = !substring.equals(substring.toLowerCase());
//				builder.append(substring.toLowerCase());
//			}
//			else {
//				// if it is not preceded by a "_" or another capitilized
//				if (!string.substring(i - 1, i).equals("_") && !previousUpper) {
//					builder.append("_");
//				}
//				previousUpper = true;
//				builder.append(substring.toLowerCase());
//			}
//		}
//		return builder.toString();
	}
	
	public static boolean isAlphanumeric(char character) {
		// capitals
		return (character >= 65 && character <= 90)
				// lowercase
				|| (character >= 97 && character <= 122)
				// numbers
				|| (character >= 48 && character <= 57);
	}
	
	public static String stringToField(String headerName) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < headerName.length(); i++) {
			if (i == 0) {
				builder.append(headerName.substring(i, i + 1).toLowerCase());
			}
			else if (!isAlphanumeric(headerName.charAt(i))) {
				builder.append(headerName.substring(i + 1, i + 2).toUpperCase());
				i++;
			}
			else {
				builder.append(headerName.substring(i, i + 1).toLowerCase());
			}
		}
		return builder.toString();
	}
	
	public static String fieldToString(String fieldName) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < fieldName.length(); i++) {
			if (i == 0) {
				builder.append(fieldName.substring(i, i + 1).toUpperCase());
			}
			else if (!fieldName.substring(i, i + 1).equals(fieldName.substring(i, i + 1).toLowerCase())) {
				builder.append(" ").append(fieldName.substring(i, i + 1).toUpperCase());
			}
			else {
				builder.append(fieldName.substring(i, i + 1).toLowerCase());
			}
		}
		return builder.toString();
	}
	
	public static void updateBrokenReferences(Resource resource, String from, String to, Charset charset) throws IOException {
		if (resource instanceof ReadableResource) {
			updateBrokenReference(resource, from, to, charset);
		}
		if (resource instanceof ResourceContainer) {
			for (Resource child : (ResourceContainer<?>) resource) {
				updateBrokenReferences(child, from, to, charset);
			}
		}
	}
	
	public static void updateBrokenReference(Resource resource, String from, String to, Charset charset) throws IOException {
		ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
		String content;
		try {
			content = new String(IOUtils.toBytes(readable), charset);
		}
		finally {
			readable.close();
		}
		// must not be prefixed with a "." because that would mean it's a partial match
		String updated = content.replaceAll("(?s)(?<!\\.)\\b" + Pattern.quote(from) + "\\b", Matcher.quoteReplacement(to));
		if (!updated.equals(content)) {
			WritableContainer<ByteBuffer> writable = ((WritableResource) resource).getWritable();
			try {
				writable.write(IOUtils.wrap(updated.getBytes(charset), true));
			}
			finally {
				writable.close();
			}
		}
	}
	
	public static List<Element<?>> getInputExtensions(Service service, Class<?> iface) {
		Method[] methods = iface.getMethods();
		if (methods.length != 1) {
			throw new IllegalArgumentException("More than 1 method found in: " + iface);
		}
		return getInputExtensions(service, methods[0]);
	}
	
	public static Method getMethod(Class<?> clazz, String name) {
		for (Method method : clazz.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		throw new IllegalArgumentException("Can not find method '" + name + "' in class: " + clazz);
	}
	
	public static List<Element<?>> getInputExtensions(Service service, Method method) {
		MethodServiceInterface iface = MethodServiceInterface.wrap(method);
		return getInputExtensions(service, iface);
	}

	public static List<Element<?>> getInputExtensions(Service service, MethodServiceInterface iface) {
		List<Element<?>> elements = new ArrayList<Element<?>>();
		ServiceInterface serviceInterface = service.getServiceInterface();
		while (serviceInterface != null && !serviceInterface.equals(iface)) {
			for (Element<?> child : serviceInterface.getInputDefinition()) {
				elements.add(child);
			}
			serviceInterface = serviceInterface.getParent();
		}
		return elements;
	}
	
	public static Entry createChildEntry(ModifiableEntry root, Artifact artifact, Artifact child) {
		String id = child.getId();
		if (id.startsWith(artifact.getId() + ".")) {
			String parentId = id.replaceAll("\\.[^.]+$", "");
			ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id.substring(artifact.getId().length() + 1), false);
			EAINode node = new EAINode();
			node.setArtifact(child);
			node.setLeaf(true);
			MemoryEntry entry = new MemoryEntry(artifact.getId(), root.getRepository(), parent, node, id, id.substring(parentId.length() + 1));
			node.setEntry(entry);
			parent.addChildren(entry);
			return entry;
		}
		return null;
	}
	
	public static boolean isBrokenReference(Repository repository, String reference) {
		// byte arrays are one of the only (should be _the_ only normally) array that we use, hence the hardcoded exception
		if (reference.equals("[B")) {
			return false;
		}
		boolean found = repository.getEntry(reference) != null && repository.getEntry(reference).isNode();
		if (!found) {
			found = repository.resolve(reference) != null;
		}
		if (!found) {
			found = DefinedServiceInterfaceResolverFactory.getInstance().getResolver().resolve(reference) != null;
		}
		if (!found) {
			found = DefinedServiceResolverFactory.getInstance().getResolver().resolve(reference) != null;
		}
		if (!found) {
			found = DefinedTypeResolverFactory.getInstance().getResolver().resolve(reference) != null;
		}
		// try multiple avenues
		if (!found) {
			try {
				found = repository.getClassLoader().loadClass(reference) != null;
			}
			catch (ClassNotFoundException e) {
				// do nothing
			}
		}
		return !found;
	}
	
	public static <T> Future<List<T>> combine(Collection<Future<T>> futures) {
		return new CombinedFuture<T>(futures);
	}
	
	@SuppressWarnings("rawtypes")
	public static class CombinedFuture<T> implements Future<List<T>> {

		private List results = new ArrayList();
		private boolean cancelled;
		private CountDownLatch latch = new CountDownLatch(1);
		private Collection<Future<T>> futures;
		
		public CombinedFuture(Collection<Future<T>> futures) {
			this.futures = futures;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (!cancelled) {
				for (Future<?> future : futures) {
					if (!future.isCancelled() && !future.isDone()) {
						future.cancel(mayInterruptIfRunning);
					}
				}
				cancelled = true;
			}
			return cancelled;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return latch.getCount() <= 0 || cancelled;
		}

		@Override
		public List<T> get() throws InterruptedException, ExecutionException {
			try {
				return get(365, TimeUnit.DAYS);
			}
			catch (TimeoutException e) {
				throw new RuntimeException(e);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			Date original = new Date();
			long msTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
			for (Future<?> future : futures) {
				long past = new Date().getTime() - original.getTime();
				results.add(future.get(msTimeout - past, TimeUnit.MILLISECONDS));
			}
			return results;
		}
		
	}
	
	public static void fireAsync(Repository repository, Object event, Object source) {
		ExecutorService executorService;
		if (repository.getServiceRunner() instanceof ExecutorServiceProvider) {
			executorService = ((ExecutorServiceProvider) repository.getServiceRunner()).getExecutorService();
		}
		else {
			executorService = ForkJoinPool.commonPool();
		}
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				repository.getEventDispatcher().fire(event, source);
			}
		});
	}
	
	public static Device getDeviceFromToken(Token token) {
		Device device = null;
		if (token != null && token instanceof DevicePrincipal) {
			device = ((DevicePrincipal) token).getDevice();
		}
		if (device == null && token != null && token.getCredentials() != null && !token.getCredentials().isEmpty()) {
			for (Principal credential : token.getCredentials()) {
				if (credential instanceof DevicePrincipal) {
					device = ((DevicePrincipal) credential).getDevice();
					if (device != null) {
						break;
					}
				}
			}
		}
		return device;
	}
	
	public static Set<String> getAllReferences(Repository repository, String nodeId) {
		Set<String> references = new HashSet<String>();
		getAllReferences(repository, nodeId, new ArrayList<String>(), references);
		return references;
	}
	
	private static void getAllReferences(Repository repository, String nodeId, List<String> searchedNodes, Set<String> result) {
		searchedNodes.add(nodeId);
		List<String> references = repository.getReferences(nodeId);
		result.addAll(references);
		for (String reference : references) {
			if (!searchedNodes.contains(reference)) {
				getAllReferences(repository, reference, searchedNodes, result);
			}
		}
	}
	
	public static Set<String> explodeReferences(Set<String> references) {
		Set<String> additional = new HashSet<String>();
		for (String reference : references) {
			if (reference == null || reference.startsWith("java.") || reference.startsWith("javax.")) {
				continue;
			}
			int index = -1;
			do {
				index = reference.lastIndexOf('.');
				if (index >= 0) {
					reference = reference.substring(0, index);
					additional.add(reference);
				}
			}
			while (index >= 0);
		}
		references.addAll(additional);
		return references;
	}
	
	public static void persist(List<Object> objects, String language, ExecutionContext context) throws ServiceException {
		Map<ComplexType, List<ComplexContent>> group = group(objects);
		for (ComplexType type : group.keySet()) {
			Map<String, List<String>> persistedFields = new HashMap<String, List<String>>();
			Map<String, String> persistedKeyFields = new HashMap<String, String>();
			Element<?> primary = null;
			for (Element<?> child : TypeUtils.getAllChildren(type)) {
				String persister = ValueUtils.getValue(PersisterProperty.getInstance(), TypeUtils.getAllProperties(child));
				if (persister != null) {
					// e.g. nabu.cms.core.providers.enricher.address:id;addressType=test;drop=street,number
					String[] split = persister.replaceAll("^(.*?);.*", "$1").split(":");
					if (split.length >= 2) {
						persister = split[0];
						persistedKeyFields.put(persister, split[1]);
					}
					if (!persistedFields.containsKey(persister)) {
						persistedFields.put(persister, new ArrayList<String>());
					}
					persistedFields.get(persister).add(child.getName());
				}
				if (primary == null) {
					Boolean isPrimary = ValueUtils.getValue(PrimaryKeyProperty.getInstance(), child.getProperties());
					if (isPrimary != null && isPrimary) {
						primary = child;
					}
				}
			}
			// if we have enriched fields, we need to enrich them
			if (!persistedFields.isEmpty()) {
				EAIResourceRepository repository = EAIResourceRepository.getInstance();
				// we need to run all enrichers separatly
				for (String persister : persistedFields.keySet()) {
					String keyField = persistedKeyFields.get(persister);
					// fall back to the primary key if not specified
					if (keyField == null && primary != null) {
						keyField = primary.getName();
					}
					Artifact resolved = repository.resolve(persister);
					// it could be a native object enricher
					if (resolved instanceof ObjectEnricher) {
						List erase = group.get(type); 
						((ObjectEnricher) resolved).persist(((DefinedType) type).getId(), language, erase, keyField, persistedFields.get(persister));
					}
					// or a service that implements the spec
					else if (resolved instanceof Service) {
						try {
							Service persisterService = (Service) resolved;
							ComplexContent input = persisterService.getServiceInterface().getInputDefinition().newInstance();
							if (type instanceof DefinedType) {
								input.set("typeId", ((DefinedType) type).getId());
							}
							input.set("language", language);
							input.set("instances", group.get(type));
							input.set("keyField", keyField);
							input.set("fields", persistedFields.get(persister));
							new ServiceRuntime(persisterService, context).run(input);
						}
						catch (Exception e) {
							throw new ServiceException("ENRICHMENT-2", "Could not run persister service " + persister, e);
						}
					}
					else {
						throw new IllegalArgumentException("Invalid persister: " + persister);
					}
				}
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void enrich(List<Object> objects, String language, ExecutionContext context) throws ServiceException {
		Map<ComplexType, List<ComplexContent>> group = group(objects);
		for (ComplexType type : group.keySet()) {
			Map<String, List<String>> enrichedFields = new HashMap<String, List<String>>();
			Map<String, String> enrichedKeyFields = new HashMap<String, String>();
			Element<?> primary = null;
			for (Element<?> child : TypeUtils.getAllChildren(type)) {
				String enricher = ValueUtils.getValue(EnricherProperty.getInstance(), TypeUtils.getAllProperties(child));
				if (enricher != null) {
					// e.g. nabu.cms.core.providers.enricher.address:id;addressType=test;drop=street,number
					String[] split = enricher.replaceAll("^(.*?);.*", "$1").split(":");
					if (split.length >= 2) {
						enricher = split[0];
						enrichedKeyFields.put(enricher, split[1]);
					}
					if (!enrichedFields.containsKey(enricher)) {
						enrichedFields.put(enricher, new ArrayList<String>());
					}
					enrichedFields.get(enricher).add(child.getName());
				}
				if (primary == null) {
					Boolean isPrimary = ValueUtils.getValue(PrimaryKeyProperty.getInstance(), child.getProperties());
					if (isPrimary != null && isPrimary) {
						primary = child;
					}
				}
			}
			// if we have enriched fields, we need to enrich them
			if (!enrichedFields.isEmpty()) {
				EAIResourceRepository repository = EAIResourceRepository.getInstance();
				// we need to run all enrichers separatly
				for (String enricher : enrichedFields.keySet()) {
					String keyField = enrichedKeyFields.get(enricher);
					// fall back to the primary key if not specified
					if (keyField == null && primary != null) {
						keyField = primary.getName();
					}
					Artifact resolved = repository.resolve(enricher);
					// it could be a native object enricher
					if (resolved instanceof ObjectEnricher) {
						List erase = group.get(type); 
						((ObjectEnricher) resolved).apply(((DefinedType) type).getId(), language, erase, keyField, enrichedFields.get(enricher));
					}
					// or a service that implements the spec
					else if (resolved instanceof Service) {
						try {
							Service enricherService = (Service) resolved;
							ComplexContent input = enricherService.getServiceInterface().getInputDefinition().newInstance();
							if (type instanceof DefinedType) {
								input.set("typeId", ((DefinedType) type).getId());
							}
							input.set("language", language);
							input.set("instances", group.get(type));
							input.set("keyField", keyField);
							input.set("fields", enrichedFields.get(enricher));
							new ServiceRuntime(enricherService, context).run(input);
						}
						catch (Exception e) {
							throw new ServiceException("ENRICHMENT-1", "Could not run enricher service " + enricher, e);
						}
					}
					else {
						throw new IllegalArgumentException("Invalid enricher: " + enricher);
					}
				}
			}
		}
	}
	
	public static Map<String, String> getEnricherProperties(ComplexType type, String field) {
		Element<?> element = type.get(field);
		if (element == null) {
			throw new IllegalArgumentException("Field does not exist: " + field);
		}
		String enricher = ValueUtils.getValue(EnricherProperty.getInstance(), TypeUtils.getAllProperties(element));
		if (enricher == null) {
			throw new IllegalArgumentException("Can not find enrichment settings for field: " + field);
		}
		Map<String, String> configuration = new HashMap<String, String>();
		for (String part : enricher.replaceAll("^[^;]+", "").split(";")) {
			String[] split = part.split("=");
			configuration.put(split[0], split[1]);
		}
		return configuration;
	}
	public static Map<String, String> getPersisterProperties(ComplexType type, String field) {
		Element<?> element = type.get(field);
		if (element == null) {
			throw new IllegalArgumentException("Field does not exist: " + field);
		}
		String persister = ValueUtils.getValue(PersisterProperty.getInstance(), TypeUtils.getAllProperties(element));
		if (persister == null) {
			throw new IllegalArgumentException("Can not find persist settings for field: " + field);
		}
		Map<String, String> configuration = new HashMap<String, String>();
		for (String part : persister.replaceAll("^[^;]+", "").split(";")) {
			String[] split = part.split("=");
			configuration.put(split[0], split[1]);
		}
		return configuration;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<ComplexType, List<ComplexContent>> group(List<Object> instances) {
		// especially for insertion, the order _is_ important, hence linked
		Map<ComplexType, List<ComplexContent>> grouped = new LinkedHashMap<ComplexType, List<ComplexContent>>();
		for (Object instance : instances) {
			if (instance != null) {
				if (!(instance instanceof ComplexContent)) {
					instance = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(instance);
					if (instance == null) {
						throw new IllegalArgumentException("Can not be cast to complex content");
					}
				}
				// if we are working with extensions, we need to figure out how the tables are laid out, we do this based on the collection name property
				// each type with its own collection name is considered to be a separate table
				ComplexType type = ((ComplexContent) instance).getType();
				
				if (!grouped.containsKey(type)) {
					grouped.put(type, new ArrayList<ComplexContent>());
				}
				grouped.get(type).add((ComplexContent) instance);
			}
		}
		return grouped;
	}

	public static List<Todo> getTodos(String id) throws IOException, ParseException {
		List<Todo> todos = new ArrayList<Todo>();
		Node node = EAIResourceRepository.getInstance().getNode(id);
		if (node != null) {
			if (node.getDescription() != null) {
				todos.addAll(ArtifactUtils.scanForTodos(id, node.getDescription()));
			}
			if (node.getSummary() != null) {
				todos.addAll(ArtifactUtils.scanForTodos(id, node.getSummary()));
			}
			if (node.getComment() != null) {
				todos.addAll(ArtifactUtils.scanForTodos(id, node.getComment()));
			}
			if (ArtifactWithTodo.class.isAssignableFrom(node.getArtifactClass())) {
				todos.addAll(((ArtifactWithTodo) node.getArtifact()).getTodos());
			}
		}
		return todos;
	}
}

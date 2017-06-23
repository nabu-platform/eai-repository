package be.nabu.eai.repository.api;

public interface Documentor<T> {
	public Class<T> getDocumentedClass();
	public Documented getDocumentation(Repository repository, T instance);
}
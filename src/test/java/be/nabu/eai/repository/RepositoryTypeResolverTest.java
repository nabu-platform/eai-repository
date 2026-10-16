package be.nabu.eai.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.Test;

import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.services.DefinedServiceInterfaceResolverFactory;
import be.nabu.libs.services.pojo.POJOInterfaceResolver;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;

public class RepositoryTypeResolverTest {

	public interface JavaContract {
		String convert(Integer value);
	}

	@Test
	public void resolvesJavaServiceInterfaceInputAndOutputTypes() {
		Repository repository = (Repository) Proxy.newProxyInstance(
			Repository.class.getClassLoader(),
			new Class<?>[] { Repository.class },
			(proxy, method, arguments) -> null);
		POJOInterfaceResolver interfaceResolver = new POJOInterfaceResolver(JavaContract.class.getClassLoader());
		DefinedServiceInterfaceResolverFactory.getInstance().addResolver(interfaceResolver);
		try {
			RepositoryTypeResolver resolver = new RepositoryTypeResolver(repository);
			String serviceId = JavaContract.class.getName() + ".convert";

			DefinedType input = resolver.resolve(serviceId + ":input");
			DefinedType output = resolver.resolve(serviceId + ":output");

			assertNotNull(input);
			assertNotNull(output);
			assertEquals(serviceId + ":input", input.getId());
			assertEquals(serviceId + ":output", output.getId());
			assertTrue(input instanceof ComplexType);
			assertTrue(output instanceof ComplexType);
			assertNotNull(((ComplexType) input).get("arg0"));
			assertNotNull(((ComplexType) output).get("response"));
		}
		finally {
			DefinedServiceInterfaceResolverFactory.getInstance().removeResolver(interfaceResolver);
		}
	}
}

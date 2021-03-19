package horseshoe.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringWriter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import horseshoe.Template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class MethodHandlerTests {

	@Test
	void testAnonymousObjectMethods() throws Throwable {
		@SuppressWarnings("unused")
		final Object obj = new Object() {
			public String foo() {
				return "hello";
			}
		};
		final AtomicReference<String> actual = new AtomicReference<>();
		final Executable e = () -> {
			actual.set(Template.load("{{ obj.foo() }}").render(Collections.singletonMap("obj", obj), new StringWriter()).toString());
		};
		if (Properties.JAVA_VERSION >= 9.0) {
			e.execute();
			assertEquals("hello", actual.get());
		} else {
			assertThrows(NoSuchMethodException.class, e);
		}
	}

}

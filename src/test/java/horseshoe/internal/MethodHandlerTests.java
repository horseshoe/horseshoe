package horseshoe.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import horseshoe.Template;
import java.io.StringWriter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class MethodHandlerTests {

	@Test
	void testAnonymousObjectMethods() throws Throwable {
		@SuppressWarnings("unused")
		final Object obj = new Object() {
			public String f() {
				return "hello";
			}
		};
		final AtomicReference<String> actual = new AtomicReference<>();
		final Executable e = () -> {
			actual.set(Template.load("{{ obj.f() }}").render(Collections.singletonMap("obj", obj), new StringWriter()).toString());
		};
		if (Properties.JAVA_VERSION >= 9.0) {
			e.execute();
			assertEquals("hello", actual.get());
		} else {
			assertThrows(NoSuchMethodException.class, e);
		}
	}

}

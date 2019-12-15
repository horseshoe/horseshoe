package horseshoe.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import horseshoe.Helper;
import horseshoe.Settings.ContextAccess;

public class ExpressionTests {

	@Test (expected = RuntimeException.class)
	public void testBadBackreachTooFar() throws ReflectiveOperationException {
		new Expression("../", false, 0);
	}

	@Test (expected = RuntimeException.class)
	public void testBadLiteralWithBackreach() throws ReflectiveOperationException {
		new Expression("../3.5", false, 1);
	}

	@Test
	public void testCommaOperator() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		assertEquals("blah", new Expression("5, 6.7, \"string-1\", \"blah\"", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null).toString());
	}

	@Test
	public void testStringConcatenation() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("cb", "bc"));
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", new Expression("\"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null).toString());
	}

}

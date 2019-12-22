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
	public void testMathOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L));
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, (Double)new Expression("(2 * `π` * r - i2)", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, (Double)new Expression("bigNum + bigNum / 2 + bigNum % 2 - `π`", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null), 0.00001);
	}

	@Test
	public void testIntegralOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L));
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, new Expression("(((r | i2) ^ 2) >> 2) << r", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null));
		assertEquals((9999999999L >>> (9999999999L & 10)) + (9999999999L >>> 10), new Expression("(bigNum >>> (bigNum & r)) + (bigNum >> r)", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null));
	}

	@Test
	public void testStringConcatenation() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("cb", "bc"));
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", new Expression("\"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null).toString());
	}

}

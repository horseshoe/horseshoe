package horseshoe.internal;

import static org.junit.Assert.assertEquals;

import java.util.regex.Pattern;

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
	public void testCompareOperators() throws ReflectiveOperationException {
		assertEquals("true, false, true, false, true, false", new Expression("(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + (5 + 8.3 == 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 == 0xFFFF) + \", \" + (\"A\" == \"B\") + \", \" + (null == null) + \", \" + (null == 5)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("false, true, false, true, false, true", new Expression("(\"a\" + \"bc\" != \"ab\" + \"c\") + \", \" + (5 + 8.3 != 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 != 0xFFFF) + \", \" + (\"A\" != \"B\") + \", \" + (null != null) + \", \" + (null != 5)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("true, true, true, true",     new Expression("(\"a\" + \"bc\" <= \"ab\" + \"c\") + \", \" + (5 + 8.3 <= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <= 0xFFFF) + \", \" + (\"A\" <= \"B\")", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("true, false, true, false",   new Expression("(\"a\" + \"bc\" >= \"ab\" + \"c\") + \", \" + (5 + 8.3 >= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >= 0xFFFF) + \", \" + (\"A\" >= \"B\")", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("false, true, false, true",   new Expression("(\"a\" + \"bc\" <  \"ab\" + \"c\") + \", \" + (5 + 8.3 <  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <  0xFFFF) + \", \" + (\"A\" <  \"B\")", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("false, false, false, false", new Expression("(\"a\" + \"bc\" >  \"ab\" + \"c\") + \", \" + (5 + 8.3 >  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >  0xFFFF) + \", \" + (\"A\" >  \"B\")", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
	}

	@Test
	public void testIntegralOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L));
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, new Expression("(((r | i2) ^ 2) >> 2) << r", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null));
		assertEquals((9999999999L >>> (9999999999L & 10)) + (9999999999L >>> 10), new Expression("(bigNum >>> (bigNum & r)) + (bigNum >> r)", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null));
		assertEquals(~(-2) - -3, new Expression("~(-2) - -3", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null));
	}

	@Test
	public void testLogicalOperators() throws ReflectiveOperationException {
		assertEquals("true", new Expression("(true && 1)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("false", new Expression("(true && !1)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("false", new Expression("(null && true)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("true", new Expression("(!null && (5 > 4))", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("true", new Expression("(null || (5 > 4))", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("true", new Expression("((\"four\".length() == 4) || false)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("false", new Expression("(null || !!null)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("true", new Expression("(true || 1)", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
	}

	@Test
	public void testMathOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L));
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, (Double)new Expression("(2 * π * r - i2)", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, (Double)new Expression("bigNum + bigNum / 2 + bigNum % 2 - π", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null), 0.00001);
	}

	@Test
	public void testPlusOperator() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("a", "a", "b", 5, "c", new StringBuilder("c"), "d", 6.5, "e", Pattern.compile("test_[0-9]+")));
		assertEquals("a11.5", new Expression("a + (b + d)", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("10.5c", new Expression("4 + d + c", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("ctest_[0-9]+56.5", new Expression("c + e + b + d", false, 0).evaluate(context, ContextAccess.CURRENT_ONLY, null).toString());
	}

	@Test
	public void testSafeOperators() throws ReflectiveOperationException {
		assertEquals("good", new Expression("null ?? \"good\"", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("good", new Expression("\"good\" ?: \"bad\"", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("good", new Expression("(null?.toString()) ?? \"good\"", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
		assertEquals("7", new Expression("(7?.toString()) ?? \"bad\"", false, 0).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null).toString());
	}

	@Test
	public void testSeparatorOperator() throws ReflectiveOperationException {
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

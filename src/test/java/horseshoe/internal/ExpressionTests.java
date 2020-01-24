package horseshoe.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import horseshoe.Helper;
import horseshoe.Settings;
import horseshoe.Settings.ContextAccess;

public class ExpressionTests {

	private static final String FILENAME = new Throwable().getStackTrace()[0].getFileName() + ":";

	@Test
	public void testArraysMaps() throws ReflectiveOperationException {
		assert((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}.getClass().isArray()", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\",}[1]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{7: \"1\", \"2\"}[7]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"blah\": \"2\"}[\"blah\"]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assert((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"].getClass().isArray()", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"][1]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[7: \"1\", \"2\",][7]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"blah\": \"2\"][\"blah\"]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null?[1] ?? \"2\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(1..5)[3]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("8", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(10..5)[2]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals(6, ((int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "10..5", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).length);
		assertEquals(1, ((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).length);

		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5, 6.7, \"string-1\", \"blah\"][1 + 2]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("string-1", ((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\", \"blah\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR))[2].toString());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\",", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(5, 6.7, \"string-1\": 7, \"blah\",)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).size());
		assertEquals(1, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "7:5,", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).size());
		assertTrue(((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[:]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).isEmpty());
		assertTrue(((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).length == 0);
		assertTrue(((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5,", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR)).length == 1);
	}

	@Test (expected = RuntimeException.class)
	public void testBadBackreachTooFar() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "../", Collections.emptyMap(), true);
	}

	@Test (expected = RuntimeException.class)
	public void testBadLiteralWithBackreach() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "../3.5", Collections.emptyMap(), true);
	}

	@Test
	public void testCompareOperators() throws ReflectiveOperationException {
		assertEquals("true, false, true, false, true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + (5 + 8.3 == 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 == 0xFFFF) + \", \" + (\"A\" == \"B\") + \", \" + (null == null) + \", \" + (null == 5)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false, true, false, true, false, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" != \"ab\" + \"c\") + \", \" + (5 + 8.3 != 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 != 0xFFFF) + \", \" + (\"A\" != \"B\") + \", \" + (null != null) + \", \" + (null != 5)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true, true, true, true",     new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <= \"ab\" + \"c\") + \", \" + (5 + 8.3 <= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <= 0xFFFF) + \", \" + (\"A\" <= \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true, false, true, false",   new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >= \"ab\" + \"c\") + \", \" + (5 + 8.3 >= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >= 0xFFFF) + \", \" + (\"A\" >= \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false, true, false, true",   new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <  \"ab\" + \"c\") + \", \" + (5 + 8.3 <  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <  0xFFFF) + \", \" + (\"A\" <  \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false, false, false, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >  \"ab\" + \"c\") + \", \" + (5 + 8.3 >  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >  0xFFFF) + \", \" + (\"A\" >  \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString() == true?.toString())", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testIntegralOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L));
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(((r | i2) ^ 2) >> 2) << r", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
		assertEquals((9999999999L >>> (9999999999L & 10)) + (9999999999L >>> 10), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(bigNum >>> (bigNum & r)) + (bigNum >> r)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
		assertEquals(~(0 - +2) - -3, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~(0 - +2) - -3", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
	}

	@Test
	public void testLogicalOperators() throws ReflectiveOperationException {
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && 1)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && !1)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null && true)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(!null && (5 > 4))", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || (5 > 4))", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "((\"four\".length() == 4) || false)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || !!null)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true || 1)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString() || true?.toString())", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testMathOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L));
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, (Double)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(2 * π * r - i2)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, (Double)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "bigNum + bigNum / 2 + bigNum % 2 - π", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR), 0.00001);
		assertEquals((int)(0x10000000L * 5280), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000 * 5280", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
		assertEquals(0x10000000L * 5280, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000L * 5280", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
	}

	@Test
	public void testNull() throws ReflectiveOperationException {
		assertEquals(null, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null.toString()", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR));
	}

	@Test
	public void testPlusOperator() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("a", "a", "b", 5, "c", new StringBuilder("c"), "d", 6.5, "e", Pattern.compile("test_[0-9]+")));
		assertEquals("a11.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a + (b + d)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("10.5c", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "1 + 5.6; +4 + d + c", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("ctest_[0-9]+56.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "c + e + b + d", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testQuotedIdentifiers() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("a\\", "a", "`b\\`", 5));
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\\\`", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`\\`b\\\\\\``", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("a5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\\\` + `\\`b\\\\\\``", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testSafeOperators() throws ReflectiveOperationException {
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null ?? \"good\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"good\" ?: \"bad\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString()) ?? \"good\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("7", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(7?.toString()) ?? \"bad\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testSeparatorOperator() throws ReflectiveOperationException {
		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5; 6.7; \"string-1\"; \"blah\";", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testStringConcatenation() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("cb", "bc"));
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\" + \"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("anull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + ab", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("bcnull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "cb + ab", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("nullbc", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "ab + cb", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("54", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5.toString() + 4.toString()", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

	@Test
	public void testTernary() throws ReflectiveOperationException {
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "true ? \"a\" : 5", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "false ? \"b\" : (1.0; 4)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("isZero", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0L * 5.6 ? \"notZero\" : \"isZero\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
		assertEquals("12345678901234", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0 ? 0 : (912345678901235; 12345678901234)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT_ONLY, null, Settings.LOG_ERRORS_TO_STDERR).toString());
	}

}

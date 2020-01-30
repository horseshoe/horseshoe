package horseshoe.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
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
	public void testArrayLookup() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("a", new int[] { 5 }, "b", new short[] { 4 }, "c", new char[] { '\n' }, "d", new byte[] { 2 }, "e", new long[] { 1 }, "f", new float[] { 0 }, "g", new double[] { -1 }, "h", new boolean[] { true }));
		assertEquals("5, 4, \n, 2, 1, 0.0, -1.0, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a[0] + \", \" + b[0] + \", \" + c[0] + \", \" + d[0] + \", \" + e[0] + \", \" + f[0] + \", \" + g[0] + \", \" + h[0]", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testArraysMaps() throws ReflectiveOperationException {
		assert((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}.getClass().isArray()", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\",}[1]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{7: \"1\", \"2\"}[7]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"blah\": \"2\"}[\"blah\"]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assert((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"].getClass().isArray()", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"][1]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[7: \"1\", \"2\",][7]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"blah\": \"2\"][\"blah\"]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null?[1] ?? \"2\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(1..5)[3]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("8", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(10..5)[2]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals(6, ((int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "10..5", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).length);
		assertEquals(1, ((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).length);
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5][5,]", Collections.emptyMap(), true);

		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5, 6.7, \"string-1\", \"blah\"][1 + 2]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("string-1", ((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\", \"blah\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER))[2].toString());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\",", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(5, 6.7, \"string-1\": 7, \"blah\",)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).size());
		assertEquals(1, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "7:5,", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).size());
		assertTrue(((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[:]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).isEmpty());
		assertTrue(((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[]", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).length == 0);
		assertTrue(((Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5,", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER)).length == 1);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayClose() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5)", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayClose2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayClose3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a, b[5", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayEmpty() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[,,]", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayEmpty2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), ",,", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayOperation() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5[5]", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayReference() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a[]", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayReference2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a[,]", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArrayReference3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a[b,,]", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBackreachAfterVar() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a../", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBackreachTooFar() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "../", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIdentifier() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "#", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIdentifier2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a #", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIdentifier3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a b", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIdentifier4() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a 1", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIdentifier5() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "1 b", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadEmpty() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadEmptyParentheses() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "()", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadEmptyParentheses2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(,)", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadEmptyParentheses3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(a,,)", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLiteral() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"blah\" a", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLiteral2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"blah\" 3.5", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLiteralWithBackreach() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "../3.5", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLocal() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a += 5", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLocal2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a = 5", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLocal3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a++", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadLocal4() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a--", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadMethodEmpty() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a(,)", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadOperator() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a +", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadStringLiteral() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"bad\\\"", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadStringLiteral2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\\q\"", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadStringLiteral3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\\u000\"", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadStringLiteral4() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\\U0000000\"", Collections.emptyMap(), true);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadTernary() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "true ? false", Collections.emptyMap(), true);
	}

	@Test
	public void testCompareOperators() throws ReflectiveOperationException {
		assertEquals("true, false, true, false, true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + (5 + 8.3 == 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 == 0xFFFF) + \", \" + (\"A\" == \"B\") + \", \" + (null == null) + \", \" + (null == 5)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false, true, false, true, false, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" != \"ab\" + \"c\") + \", \" + (5 + 8.3 != 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 != 0xFFFF) + \", \" + (\"A\" != \"B\") + \", \" + (null != null) + \", \" + (null != 5)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true, true, true, true",     new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <= \"ab\" + \"c\") + \", \" + (5 + 8.3 <= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <= 0xFFFF) + \", \" + (\"A\" <= \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true, false, true, false",   new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >= \"ab\" + \"c\") + \", \" + (5 + 8.3 >= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >= 0xFFFF) + \", \" + (\"A\" >= \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false, true, false, true",   new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <  \"ab\" + \"c\") + \", \" + (5 + 8.3 <  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <  0xFFFF) + \", \" + (\"A\" <  \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false, false, false, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >  \"ab\" + \"c\") + \", \" + (5 + 8.3 >  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >  0xFFFF) + \", \" + (\"A\" >  \"B\")", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString() == true?.toString())", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testEquals() throws ReflectiveOperationException {
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true), "");
		assertEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true));
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b", Collections.emptyMap(), true));
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber() + 1, "a", Collections.emptyMap(), true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true));
	}

	@Test
	public void testIntegralOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L));
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(((r | i2) ^ 2) >> 2) << r", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
		assertEquals((9999999999L >>> (9999999999L & 10)) + (9999999999L >>> 10), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(bigNum >>> (bigNum & r)) + (bigNum >> r)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
		assertEquals(~(0 - +2) - -3, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~(0 - +2) - -3", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
	}

	@Test
	public void testLocals() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L));
		assertTrue(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "π = 3.14159265358979311599796346854; r = 4; \"r = \" + r + \", d = \" + (r * 2) + \", a = \" + (π * r * r)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString().startsWith("r = 4, d = 8, a = 50.26"));
		assertEquals(418.87902047863909846168578443727, (Double)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "r = 4; 4 / 3.0 * π * ./r *./r", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER), 0.00001);
		assertEquals("blah32", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a = 1; b = 2; c = 3; d = 4; e = 5; e *= b; c -= a >>= e %= b; a = (c++ == 2) ? null : 0xFF; b -= (d-- == 4) ? -1 : 5; b--; a ??= \"blah\" + c++ + b", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a = 0; a++ == 0 ? (a == 1 ? \"good\" : \"bad\") : \"bad2\"", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertNull(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b == 0 ? (a = 1) : 4; a", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));

		{
			final Object result[] = new Object[2000];
			final StringBuilder sb = new StringBuilder("");
			final StringBuilder resultSB = new StringBuilder("[");

			for (int i = 0; i < result.length; i++) {
				result[i] = i;
				sb.append('a').append(i).append('=').append(i).append(';');
				resultSB.append('a').append(i).append(',');
			}

			assertArrayEquals(result, (Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), sb.append(resultSB).append(']').toString(), Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
		}
	}

	@Test
	public void testLogicalOperators() throws ReflectiveOperationException {
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && 1)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && !1)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null && true)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(!null && (5 > 4))", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || (5 > 4))", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "((\"four\".length() == 4) || false)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || !!null)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true || 1)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString() || true?.toString())", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testMathOperators() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L));
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, (Double)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(2 * π * r - i2)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, (Double)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "bigNum + bigNum / 2 + bigNum % 2 - π", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER), 0.00001);
		assertEquals((int)(0x10000000L * 5280), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000 * 5280", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
		assertEquals(0x10000000L * 5280, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000L * 5280", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
	}

	@Test
	public void testNull() throws ReflectiveOperationException {
		assertEquals(null, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null.toString()", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER));
	}

	@Test
	public void testPlusOperator() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("a", "a", "b", 5, "c", new StringBuilder("c"), "d", 6.5, "e", Pattern.compile("test_[0-9]+")));
		assertEquals("a11.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a + (b + d)", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("10.5c", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "1 + 5.6; +4 + d + c", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("ctest_[0-9]+56.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "c + e + b + d", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testQuotedIdentifiers() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("a\\", "a", "`b\\`", 5));
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\\\`", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`\\`b\\\\\\``", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("a5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\\\` + `\\`b\\\\\\``", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testSafeOperators() throws ReflectiveOperationException {
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null ?? \"good\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"good\" ?: \"bad\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString()) ?? \"good\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("7", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(7?.toString()) ?? \"bad\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testSeparatorOperator() throws ReflectiveOperationException {
		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5; 6.7; \"string-1\"; \"blah\";", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testStringConcatenation() throws ReflectiveOperationException {
		final PersistentStack<Object> context = new PersistentStack<>();
		context.push(Helper.loadMap("cb", "bc"));
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\" + \"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("anull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + ab", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("bcnull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "cb + ab", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("nullbc", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "ab + cb", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("54", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5.toString() + 4.toString()", Collections.emptyMap(), true).evaluate(context, ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

	@Test
	public void testTernary() throws ReflectiveOperationException {
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "true ? \"a\" : 5", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "false ? \"b\" : (1.0; 4)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("isZero", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0L * 5.6 ? \"notZero\" : \"isZero\"", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
		assertEquals("12345678901234", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0 ? 0 : (912345678901235; 12345678901234)", Collections.emptyMap(), true).evaluate(new PersistentStack<>(), ContextAccess.CURRENT, null, Settings.STDERR_ERROR_LOGGER).toString());
	}

}

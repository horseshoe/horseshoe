package horseshoe.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import horseshoe.Helper;
import horseshoe.RenderContext;
import horseshoe.Settings;
import horseshoe.Settings.ContextAccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressionTests {

	private static final String FILENAME = new Throwable().getStackTrace()[0].getFileName() + ":";
	private static final Map<String, Expression> EMPTY_EXPRESSIONS_MAP = Collections.<String, Expression>emptyMap();

	@Test
	void testArrayLookup() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a", new int[] { 5 }, "b", new short[] { 4 }, "c", new char[] { '\n' }, "d", new byte[] { 2 }, "e", new long[] { 1 }, "f", new float[] { 0 }, "g", new double[] { -1 }, "h", new boolean[] { true }, "i", new Object[] { "Blue" });
		assertEquals("5, 4, \n, 2, 1, 0.0, -1.0, true, Blue", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a[0] + \", \" + b[0] + \", \" + c[0] + \", \" + d[0] + \", \" + e[0] + \", \" + f[0] + \", \" + g[0] + \", \" + h[0] + \", \" + i[0]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testArrayLookupRange() throws ReflectiveOperationException {
		assertArrayEquals(new int[] { 5, 4 }, (int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a[0:2]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("a", new int[] { 5, 4 }))));
		assertArrayEquals(new short[] { 4 }, (short[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("b", new short[] { 4, 3 }))));
		assertArrayEquals(new char[] { '\n' }, (char[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "c[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("c", new char[] { '\n', 'o' }))));
		assertArrayEquals(new byte[] { 2 }, (byte[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "d[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("d", new byte[] { 2, 1 }))));
		assertArrayEquals(new long[] { 1 }, (long[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "e[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("e", new long[] { 1, 0 }))));
		assertArrayEquals(new float[] { 0.0f }, (float[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "f[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("f", new float[] { 0, -1 }))), 0.0001f);
		assertArrayEquals(new double[] { -1.0 }, (double[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "g[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("g", new double[] { -1, -2 }))), 0.0001);
		assertArrayEquals(new boolean[] { true }, (boolean[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "h[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("h", new boolean[] { true, false }))));
		assertArrayEquals(new Object[] { "Blue" }, (Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "i[0:1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("i", new Object[] { "Blue", "Red" }))));
	}

	@Test
	void testBadRegularExpression() throws ReflectiveOperationException {
		final String name = FILENAME + new Throwable().getStackTrace()[0].getLineNumber();
		assertThrows(PatternSyntaxException.class, () -> new Expression(name, "~/^abc(.\\u0065/.matcher('abcde').matches()", EMPTY_EXPRESSIONS_MAP, true));
	}

	@ParameterizedTest
	@ValueSource(strings = { "[5)", "[5", "a, b[5", "[,,]", ",,", "5[5]", "a[]", "a[,]", "a[b,,]", "a =", "a ; = 2", "a../", "../", "", "()", "(,)", "(a,,)", "#", "a #", "a b", "a 1", "1 b", "\"blah\" a", "\"blah\" 3.5", "blah.3.5", "..\\3.5", "a += 5", "./a = 5", "./a++", "a--", "a = 0; a-- = 1", "a(,)", "a +", "call(/..)", "call(/.a)", "call(/a())", "\"bad\\\"", "\"\\q\"", "\"\\u000\"", "\"\\U0000000\"", "true ? false" })
	void testBadSyntax(final String expression) throws ReflectiveOperationException {
		final String name = FILENAME + new Throwable().getStackTrace()[0].getLineNumber();
		assertThrows(IllegalArgumentException.class, () -> new Expression(name, expression, EMPTY_EXPRESSIONS_MAP, true));
	}

	@Test
	void testComments() throws ReflectiveOperationException {
		assertEquals("true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "// Ignore this comment\n(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + /* Ignore multi-line comment with code\n '; ' + */ (5 + 8.3 == 5.31 + 8) /* Trailing double comment */ // Second comment", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testCompareOperators() throws ReflectiveOperationException {
		assertEquals("true, false, true, false, true, false, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + (5 + 8.3 == 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 == 0xFFFF) + \", \" + (\"A\" == \"B\") + \", \" + (null == null) + \", \" + (null == 5) + ', ' + (~@'java.time.DayOfWeek'.SUNDAY == ~@'java.time.DayOfWeek'.SUNDAY.getDeclaringClass().SUNDAY)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT).addLoadableClasses(java.time.DayOfWeek.class), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false, true, false, true, false, true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" != \"ab\" + \"c\") + \", \" + (5 + 8.3 != 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 != 0xFFFF) + \", \" + (\"A\" != \"B\") + \", \" + (null != null) + \", \" + (null != 5) + ', ' + (~@'java.time.DayOfWeek'.SUNDAY != ~@'java.time.DayOfWeek'.SUNDAY)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT).addLoadableClasses(java.time.DayOfWeek.class), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true, true, true, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <= \"ab\" + \"c\") + \", \" + (5 + 8.3 <= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <= 0xFFFF) + \", \" + (\"A\" <= \"B\")", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true, false, true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >= \"ab\" + \"c\") + \", \" + (5 + 8.3 >= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >= 0xFFFF) + \", \" + (\"A\" >= \"B\")", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false, true, false, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <  \"ab\" + \"c\") + \", \" + (5 + 8.3 <  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <  0xFFFF) + \", \" + (\"A\" <  \"B\")", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false, false, false, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >  \"ab\" + \"c\") + \", \" + (5 + 8.3 >  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >  0xFFFF) + \", \" + (\"A\" >  \"B\")", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString() == true?.toString())", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + \"b\" == \"ab\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals(0, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + \"b\" <=> \"ab\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext())).intValue());
		assertTrue(0 > (Integer)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null <=> \"ab\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext()));
		assertTrue(0 < (Integer)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + \"b\" <=> null", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext()));
	}

	@Test
	void testDie() throws ReflectiveOperationException {
		final Expression expression = new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "☠ \"Should die with error\"; \"Did not die\"", EMPTY_EXPRESSIONS_MAP, true);
		final RenderContext context = new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap());
		assertThrows(HaltRenderingException.class, () -> expression.evaluate(context));
	}

	@Test
	void testDie2() throws ReflectiveOperationException {
		final Expression expression = new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~:< 'Should die with error'; 'Did not die'", EMPTY_EXPRESSIONS_MAP, true);
		final RenderContext context = new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap());
		assertThrows(HaltRenderingException.class, () -> expression.evaluate(context));
	}

	@Test
	void testEquals() throws ReflectiveOperationException {
		assertNotEquals("", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", EMPTY_EXPRESSIONS_MAP, true));
		assertEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", EMPTY_EXPRESSIONS_MAP, true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", EMPTY_EXPRESSIONS_MAP, true));
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", EMPTY_EXPRESSIONS_MAP, true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b", EMPTY_EXPRESSIONS_MAP, true));
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber() + 1, "a", EMPTY_EXPRESSIONS_MAP, true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", EMPTY_EXPRESSIONS_MAP, true));
	}

	@Test
	void testIdentifier() {
		assertNotEquals(new Identifier("blah", 1), new Object());
		assertNotEquals(new Identifier("blah", 2), new Identifier("blah", 1));
	}

	@Test
	void testIn() throws ReflectiveOperationException {
		assertTrue((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'Test' in ['Test', 'Retest']", EMPTY_EXPRESSIONS_MAP, true).evaluate());
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'test' in ['Test', 'Retest']", EMPTY_EXPRESSIONS_MAP, true).evaluate());
	}

	@Test
	void testIntegralOperators() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L);
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(((+r | i2) ^ 2) >> 2) << r", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals((9999999999L >>> (9999999999L & 10)) + -10 + (9999999999L >>> 10), ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(bigNum >>> (bigNum & r)) + -r + (bigNum >> r)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals(~(0 - +2) - -3, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~(0 - +2) - -3", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals("name", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a = 'prefix:name'; a.substring(a.indexOf(':') + 1)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
	}

	@Test
	void testLists() throws ReflectiveOperationException {
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}.getClass().isArray()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertNull(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}[1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}['2']", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\",][1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals(Arrays.asList(3, 4), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", 2, 3, 4,][2:]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Arrays.asList(3, 2, "1"), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", 2, 3, 4,][2:<]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"].getClass().isArray()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"][1]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null?[1] ?? \"2\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(1..5)[3]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("7", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(5..<10)[2]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertArrayEquals(new int[] { 10, 9, 8, 7, 6, 5 }, ((int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "10..5", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))));
		assertArrayEquals(new int[] { 10, 9, 8, 7, 6 }, ((int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "10..<5", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))));
		assertEquals(1, ((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertNull(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5][5,]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));

		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5, 6.7, \"string-1\", \"blah\"][1 + 2]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("string-1", ((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\", \"blah\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).get(2).toString());
		assertTrue(((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).isEmpty());
		assertEquals(1, ((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5,", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
	}

	@Test
	void testLiterals() throws ReflectiveOperationException {
		assertEquals(-255, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0xFF", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(255, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0xFF", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(-255L, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0xFFL", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(255L, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0xffl", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2d", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2e0", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2f", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2D", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2F", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2.0", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2.e-0", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(0.5, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0.5", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(-0.5, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0.5", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(1.5, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x3p-1", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(-9.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0x1.2p3", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2_600.452_864, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2'600.452'864f", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Double.NEGATIVE_INFINITY, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-Infinity", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Double.POSITIVE_INFINITY, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "+Infinity", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
	}

	@Test
	void testLocals() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L);
		assertTrue(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "π = 3.14159265358979311599796346854; r /* radius */ = 4; \"r = \" + r + \", d = \" + (r * 2) + \", a = \" + (π * r * r)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString().startsWith("r = 4, d = 8, a = 50.26"));
		assertEquals(418.87902047863909846168578443727, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "r = 4; 4 / 3.0 * π * ./r *./r", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertNull(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b == 0 ? (a = 1) : 4; a", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));

		{
			final int length = 250;
			final ArrayList<Object> result = new ArrayList<>(length);
			final StringBuilder sb = new StringBuilder(length * 7);
			final StringBuilder resultSB = new StringBuilder(length * 5).append("[");

			for (int i = 0; i < length; i++) {
				result.add(i, i);
				sb.append('a').append(i).append('=').append(i).append(';');
				resultSB.append('a').append(i).append(',');
			}

			assertEquals(result, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), sb.append(resultSB).append(']').toString(), EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
		}
	}

	@Test
	void testLogicalOperators() throws ReflectiveOperationException {
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && 1)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && !1)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null && true)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(!null && (5 > 4))", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || (5 > 4))", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "((\"four\".length() == 4) || false)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || !!null)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true || 1)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString() || true?.toString())", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testMathOperators() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L);
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(2 * π * r - i2)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "bigNum + bigNum / 2 + bigNum % 2 - π", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertEquals((int)(0x10000000L * 5280), ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000 * 5280", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals(0x10000000L * 5280, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000L * 5280", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).longValue());
		assertEquals(-Math.pow(2, 6), ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-i2 ** 6", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0);
	}

	@Test
	void testMaps() throws ReflectiveOperationException {
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{7: \"1\", \"2\"}[7]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"blah\": \"2\"}[\"blah\"]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[7: \"1\", \"2\",][7]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"blah\": \"2\"][\"blah\"]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());

		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\",", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(5, 6.7, \"string-1\": 7, \"blah\",)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(1, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "7:5,", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(1, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "7:5", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertTrue(((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[:]", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).isEmpty());
	}

	@Test
	void testNull() throws ReflectiveOperationException {
		assertEquals(null, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null.toString()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
	}

	@Test
	void testOperand() throws ReflectiveOperationException {
		assertEquals("", new Operand(Object.class, null).toString());
		assertNotEquals("", new Operand(Object.class, new MethodBuilder().pushConstant(0)).toString());
	}

	@Test
	void testPatternOperators() throws ReflectiveOperationException {
		assertTrue((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'0xdeadBEEFd134' ==~ ~/0x[0-9a-fA-F]+/", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext()));
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'0x' + 'dead' + 'BEEF' + 'd134' ==~ '[0-9a-fA-F]' + '{2}'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext()));
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'deadBEEFd134' =~ ~/0x[0-9a-fA-F]+/", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext()));
		assertTrue((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'dead' + 'BEEF' + 'd134' =~ '[0-9a-fA-F]' + '{2}'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext()));
	}

	@Test
	void testPlusOperator() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a", "a", "b", 5, "c", "c", "d", 6.5, "e", Pattern.compile("test_[0-9]+"));
		assertEquals("a11.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a + (b + d)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("10.5c", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "1 + 5.6; +4 + d + c", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("ctest_[0-9]+56.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "c + e + b + d", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testQuotedIdentifiers() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a\\", "a", "`b\\`", 5);
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\`", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "```b\\```", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("a5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\` + ```b\\```", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testRegularExpression() throws ReflectiveOperationException {
		assertEquals("true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/^abc.\\u0065/.matcher('abcde').matches() + ', ' + ~/^abc.\\u0065/.matcher('abcdef').matches()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true, true, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~//.matcher('').matches() + ', ' + ~/\\//.matcher('/').matches() + ', ' + ~/\\\\/.matcher('\\').matches()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/\\d/.matcher(\"\\u0660\").matches()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/(?-U)\\d/.matcher(\"\\u0660\").matches()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testReturn() throws ReflectiveOperationException {
		assertEquals("BlueGreen", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "#^'Blue' + 'Green'; 'Yellow'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testSafeOperators() throws ReflectiveOperationException {
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null ?? \"good\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"good\" ?: \"bad\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.toString()) ?? \"good\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null.?toString()) ?? \"good\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(7.?badMethod()) ?? \"good\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("7", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(7?.toString()) ?? \"bad\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testSeparatorOperator() throws ReflectiveOperationException {
		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5; 6.7; \"string-1\"; \"blah\";", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testStringConcatenation() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("cb", "bc");
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\" + \"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("anull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + ab", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("bcnull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "cb + ab", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("nullbc", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "ab + cb", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("54", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5.toString() + 4.toString()", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testStringLiterals() throws ReflectiveOperationException {
		assertEquals("d \\\"\'\b\t\n\f\r\0\0πƪāĂ\t", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\0\\x0π\\x1Aa\\u0101\\U00000102\\x9\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("d \\\\\\\"\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'d \\\\\\\"\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("a 'single' quoted string", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'a ''single'' quoted string'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testTernary() throws ReflectiveOperationException {
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "true ? \"a\" : 5", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "false ? \"b\" : (1.0; 4)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("isZero", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0L * 5.6 ? \"notZero\" : \"isZero\"", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("12345678901234", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0 ? 0 : (912345678901235; 12345678901234)", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals(1, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "pain > 2 ? 'moderate' : #^pain", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 1))));
		assertEquals(Collections.singletonMap(1, 2), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "test == 0 ? null ?: 'zero' : test : 2", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("test", 1))));
		assertEquals("extreme", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'good' : 'moderate'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 10))).toString());
		assertEquals("bad", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'good' : 'moderate'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 7))).toString());
		assertEquals("moderate", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'good' : 'moderate'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 4))).toString());
		assertEquals("low", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'low' : 'moderate'", EMPTY_EXPRESSIONS_MAP, true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 0))).toString());
	}

}

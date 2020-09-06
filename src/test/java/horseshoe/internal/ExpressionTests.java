package horseshoe.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import org.junit.Test;

public class ExpressionTests {

	private static final String FILENAME = new Throwable().getStackTrace()[0].getFileName() + ":";

	@Test
	public void testArrayLookup() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a", new int[] { 5 }, "b", new short[] { 4 }, "c", new char[] { '\n' }, "d", new byte[] { 2 }, "e", new long[] { 1 }, "f", new float[] { 0 }, "g", new double[] { -1 }, "h", new boolean[] { true }, "i", new Object[] { "Blue" });
		assertEquals("5, 4, \n, 2, 1, 0.0, -1.0, true, Blue", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a[0] + \", \" + b[0] + \", \" + c[0] + \", \" + d[0] + \", \" + e[0] + \", \" + f[0] + \", \" + g[0] + \", \" + h[0] + \", \" + i[0]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	public void testArrayLookupRange() throws ReflectiveOperationException {
		assertArrayEquals(new int[] {5, 4}, (int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a[0:3]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("a", new int[] { 5, 4 }))));
		assertArrayEquals(new short[] {4}, (short[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("b", new short[] { 4, 3 }))));
		assertArrayEquals(new char[] {'\n'}, (char[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "c[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("c", new char[] { '\n', 'o' }))));
		assertArrayEquals(new byte[] {2}, (byte[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "d[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("d", new byte[] { 2, 1 }))));
		assertArrayEquals(new long[] {1}, (long[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "e[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("e", new long[] { 1, 0 }))));
		assertArrayEquals(new float[] {0.0f}, (float[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "f[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("f", new float[] { 0, -1 }))), 0.0001f);
		assertArrayEquals(new double[] {-1.0}, (double[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "g[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("g", new double[] { -1, -2 }))), 0.0001);
		assertArrayEquals(new boolean[] {true}, (boolean[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "h[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("h", new boolean[] { true, false }))));
		assertArrayEquals(new Object[] {"Blue"}, (Object[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "i[0:1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("i", new Object[] { "Blue", "Red" }))));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayClose() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5)", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayClose2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayClose3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a, b[5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayEmpty() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[,,]", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayEmpty2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), ",,", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayOperation() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5[5]", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayReference() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a[]", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayReference2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a[,]", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArrayReference3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a[b,,]", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadAssignment() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a =", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadAssignment2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a ; = 2", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadBackreachAfterVar() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a../", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadBackreachTooFar() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "../", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadEmpty() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadEmptyParentheses() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "()", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadEmptyParentheses2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(,)", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadEmptyParentheses3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(a,,)", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadIdentifier() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "#", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadIdentifier2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a #", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadIdentifier3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a b", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadIdentifier4() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a 1", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadIdentifier5() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "1 b", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLiteral() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"blah\" a", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLiteral2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"blah\" 3.5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLiteralAfterNav() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "blah.3.5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLiteralWithBackreach() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "..\\3.5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLocal() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a += 5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLocal2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a = 5", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLocal3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "./a++", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLocal4() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a--", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadLocal5() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a = 0; a-- = 1", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadMethodEmpty() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a(,)", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadOperator() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a +", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadPrefix() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "call(/..)", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadPrefix2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "call(/.a)", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadPrefix3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "call(/a())", Collections.emptyMap(), true);
	}

	@Test(expected = PatternSyntaxException.class)
	public void testBadRegularExpression() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/^abc(.\\u0065/.matcher('abcde').matches()", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadStringLiteral() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"bad\\\"", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadStringLiteral2() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\\q\"", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadStringLiteral3() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\\u000\"", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadStringLiteral4() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\\U0000000\"", Collections.emptyMap(), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadTernary() throws ReflectiveOperationException {
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "true ? false", Collections.emptyMap(), true);
	}

	@Test
	public void testComments() throws ReflectiveOperationException {
		assertEquals("true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "// Ignore this comment\n(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + /* Ignore multi-line comment with code\n '; ' + */ (5 + 8.3 == 5.31 + 8) /* Trailing double comment */ // Second comment", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testCompareOperators() throws ReflectiveOperationException {
		assertEquals("true, false, true, false, true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + (5 + 8.3 == 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 == 0xFFFF) + \", \" + (\"A\" == \"B\") + \", \" + (null == null) + \", \" + (null == 5)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false, true, false, true, false, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" != \"ab\" + \"c\") + \", \" + (5 + 8.3 != 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 != 0xFFFF) + \", \" + (\"A\" != \"B\") + \", \" + (null != null) + \", \" + (null != 5)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true, true, true, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <= \"ab\" + \"c\") + \", \" + (5 + 8.3 <= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <= 0xFFFF) + \", \" + (\"A\" <= \"B\")", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true, false, true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >= \"ab\" + \"c\") + \", \" + (5 + 8.3 >= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >= 0xFFFF) + \", \" + (\"A\" >= \"B\")", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false, true, false, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" <  \"ab\" + \"c\") + \", \" + (5 + 8.3 <  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <  0xFFFF) + \", \" + (\"A\" <  \"B\")", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false, false, false, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(\"a\" + \"bc\" >  \"ab\" + \"c\") + \", \" + (5 + 8.3 >  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >  0xFFFF) + \", \" + (\"A\" >  \"B\")", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.?toString() == true?.?toString())", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + \"b\" == \"ab\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test(expected = HaltRenderingException.class)
	public void testDie() throws ReflectiveOperationException {
		assertEquals("Should have died", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "☠ \"Should die with error\"; \"Did not die\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test(expected = HaltRenderingException.class)
	public void testDie2() throws ReflectiveOperationException {
		assertEquals("Should have died", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~:< 'Should die with error'; \"Did not die\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testEquals() throws ReflectiveOperationException {
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true), "");
		assertEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true));
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b", Collections.emptyMap(), true));
		assertNotEquals(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber() + 1, "a", Collections.emptyMap(), true), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a", Collections.emptyMap(), true));
	}

	@Test
	public void testIdentifier() {
		assertNotEquals(new Identifier("blah", 1), new Object());
		assertNotEquals(new Identifier("blah", 2), new Identifier("blah", 1));
	}

	@Test
	public void testIntegralOperators() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L);
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(((+r | i2) ^ 2) >> 2) << r", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals((9999999999L >>> (9999999999L & 10)) + -10 + (9999999999L >>> 10), ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(bigNum >>> (bigNum & r)) + -r + (bigNum >> r)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals(~(0 - +2) - -3, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~(0 - +2) - -3", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals("name", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a = 'prefix:name'; a.substring(a.indexOf(':') + 1)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
	}

	@Test
	public void testListsMaps() throws ReflectiveOperationException {
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}.getClass().isArray()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertNull(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}[1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"2\"}['2']", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\",][1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals(Arrays.asList(3, 4), new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", 2, 3, 4,][2:~@Integer.MAX_VALUE]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{7: \"1\", \"2\"}[7]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "{\"1\", \"blah\": \"2\"}[\"blah\"]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertFalse((Boolean)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"].getClass().isArray()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"2\"][1]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("1", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[7: \"1\", \"2\",][7]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[\"1\", \"blah\": \"2\"][\"blah\"]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("2", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null?[?1] ?? \"2\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(1..5)[3]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("8", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(10..5)[2]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals(6, ((int[])new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "10..5", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).length);
		assertEquals(1, ((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
		new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[10..5][5,]", Collections.emptyMap(), true);

		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[5, 6.7, \"string-1\", \"blah\"][1 + 2]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("string-1", ((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\", \"blah\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).get(2).toString());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5, 6.7, \"string-1\": 7, \"blah\",", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
		assertEquals(4, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(5, 6.7, \"string-1\": 7, \"blah\",)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
		assertEquals(1, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "7:5,", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
		assertEquals(1, ((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "7:5", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
		assertTrue(((Map<?, ?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[:]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).isEmpty());
		assertTrue(((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "[]", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).isEmpty());
		assertEquals(1, ((List<?>)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5,", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap()))).size());
	}

	@Test
	public void testLiterals() throws ReflectiveOperationException {
		assertEquals(-255, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0xFF", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(255, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0xFF", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(-255L, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0xFFL", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(255L, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0xffl", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2d", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2e0", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2f", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2D", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2F", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2.0", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(2.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "2.e-0", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(0.5, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0.5", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(-0.5, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0.5", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(1.5, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x3p-1", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(-9.0, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-0x1.2p3", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(Double.NEGATIVE_INFINITY, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "-Infinity", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
		assertEquals(Double.POSITIVE_INFINITY, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "+Infinity", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
	}

	@Test
	public void testLocals() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L);
		assertTrue(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "π = 3.14159265358979311599796346854; r /* radius */ = 4; \"r = \" + r + \", d = \" + (r * 2) + \", a = \" + (π * r * r)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString().startsWith("r = 4, d = 8, a = 50.26"));
		assertEquals(418.87902047863909846168578443727, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "r = 4; 4 / 3.0 * π * ./r *./r", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertNull(new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "b == 0 ? (a = 1) : 4; a", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));

		{
			final int length = 2000;
			final ArrayList<Object> result = new ArrayList<>(length);
			final StringBuilder sb = new StringBuilder("");
			final StringBuilder resultSB = new StringBuilder("[");

			for (int i = 0; i < length; i++) {
				result.add(i, i);
				sb.append('a').append(i).append('=').append(i).append(';');
				resultSB.append('a').append(i).append(',');
			}

			assertEquals(result, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), sb.append(resultSB).append(']').toString(), Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
		}
	}

	@Test
	public void testLogicalOperators() throws ReflectiveOperationException {
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && 1)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true && !1)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null && true)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(!null && (5 > 4))", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || (5 > 4))", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "((\"four\".length() == 4) || false)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null || !!null)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(true || 1)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.?toString() || true?.?toString())", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testMathOperators() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L);
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(2 * π * r - i2)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "bigNum + bigNum / 2 + bigNum % 2 - π", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertEquals((int)(0x10000000L * 5280), ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000 * 5280", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals(0x10000000L * 5280, ((Number)new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0x10000000L * 5280", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).longValue());
	}

	@Test
	public void testNull() throws ReflectiveOperationException {
		assertEquals(null, new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null.toString()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())));
	}

	@Test
	public void testOperand() throws ReflectiveOperationException {
		assertEquals("", new Operand(Object.class, null).toString());
		assertNotEquals("", new Operand(Object.class, new MethodBuilder().pushConstant(0)).toString());
	}

	@Test
	public void testPlusOperator() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a", "a", "b", 5, "c", "c", "d", 6.5, "e", Pattern.compile("test_[0-9]+"));
		assertEquals("a11.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "a + (b + d)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("10.5c", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "1 + 5.6; +4 + d + c", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("ctest_[0-9]+56.5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "c + e + b + d", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	public void testQuotedIdentifiers() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a\\", "a", "`b\\`", 5);
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\`", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "```b\\```", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("a5", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "`a\\` + ```b\\```", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	public void testRegularExpression() throws ReflectiveOperationException {
		assertEquals("true, false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/^abc.\\u0065/.matcher('abcde').matches() + ', ' + ~/^abc.\\u0065/.matcher('abcdef').matches()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true, true, true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~//.matcher('').matches() + ', ' + ~/\\//.matcher('/').matches() + ', ' + ~/\\\\/.matcher('\\').matches()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("true", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/\\d/.matcher(\"\\u0660\").matches()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("false", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "~/(?-U)\\d/.matcher(\"\\u0660\").matches()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testReturn() throws ReflectiveOperationException {
		assertEquals("BlueGreen", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "#^'Blue' + 'Green'; 'Yellow'", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testSafeOperators() throws ReflectiveOperationException {
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "null ?? \"good\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"good\" ?: \"bad\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("good", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(null?.?toString()) ?? \"good\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("7", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "(7?.?toString()) ?? \"bad\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testSeparatorOperator() throws ReflectiveOperationException {
		assertEquals("blah", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5; 6.7; \"string-1\"; \"blah\";", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testStringConcatenation() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("cb", "bc");
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"\" + \"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("anull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"a\" + ab", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("bcnull", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "cb + ab", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("nullbc", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "ab + cb", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("54", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "5.toString() + 4.toString()", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	public void testStringLiterals() throws ReflectiveOperationException {
		assertEquals("d \\\"\'\b\t\n\f\r\0\0πƪāĂ\t", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "\"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\0\\x0π\\x1Aa\\u0101\\U00000102\\x9\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("d \\\\\\\"\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'d \\\\\\\"\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9'", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("a 'single' quoted string", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "'a ''single'' quoted string'", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

	@Test
	public void testTernary() throws ReflectiveOperationException {
		assertEquals("a", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "true ? \"a\" : 5", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("4", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "false ? \"b\" : (1.0; 4)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("isZero", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0L * 5.6 ? \"notZero\" : \"isZero\"", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
		assertEquals("12345678901234", new Expression(FILENAME + new Throwable().getStackTrace()[0].getLineNumber(), "0 ? 0 : (912345678901235; 12345678901234)", Collections.emptyMap(), true).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.emptyMap())).toString());
	}

}

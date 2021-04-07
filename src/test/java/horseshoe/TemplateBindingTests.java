package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;

import horseshoe.Settings.ContextAccess;
import horseshoe.internal.TemplateBinding;

import org.junit.jupiter.api.Test;

class TemplateBindingTests {

	@Test
	void testBinding() throws IOException, LoadException {
		assertEquals("test", new TemplateBinding("test", 0, 0).getName());
		assertEquals("1", new TemplateLoader().load("Binding", "{{ templateBinding := 1 }}{{ templateBinding }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testManyBindings() throws IOException, LoadException {
		assertEquals("1234567891011121314151617181920212223242526", new TemplateLoader().load("Binding", "{{ a := 1 }}{{ a }}{{ b := 2 }}{{ b }}{{ c := 3 }}{{ c }}{{ d := 4 }}{{ d }}{{ e := 5 }}{{ e }}{{ f := 6 }}{{ f }}{{ g := 7 }}{{ g }}{{ h := 8 }}{{ h }}{{ i := 9 }}{{ i }}{{ j := 10 }}{{ j }}{{ k := 11 }}{{ k }}{{ l := 12 }}{{ l }}{{ m := 13 }}{{ m }}{{ n := 14 }}{{ n }}{{ o := 15 }}{{ o }}{{ p := 16 }}{{ p }}{{ q := 17 }}{{ q }}{{ r := 18 }}{{ r }}{{ s := 19 }}{{ s }}{{ t := 20 }}{{ t }}{{ u := 21 }}{{ u }}{{ v := 22 }}{{ v }}{{ w := 23 }}{{ w }}{{ x := 24 }}{{ x }}{{ y := 25 }}{{ y }}{{ z := 26 }}{{ z }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testManyNestedBindings() throws IOException, LoadException {
		assertEquals("026", new TemplateLoader().load("Binding", "{{ a := 0 }}{{< a }}{{ a }}{{ a := a + 1 }}{{< b }}{{ a := a + 1 }}{{< c }}{{ a := a + 1 }}{{< d }}{{ a := a + 1 }}{{< e }}{{ a := a + 1 }}{{< f }}{{ a := a + 1 }}{{< g }}{{ a := a + 1 }}{{< h }}{{ a := a + 1 }}{{< i }}{{ a := a + 1 }}{{< j }}{{ a := a + 1 }}{{< k }}{{ a := a + 1 }}{{< l }}{{ a := a + 1 }}{{< m }}{{ a := a + 1 }}{{< n }}{{ a := a + 1 }}{{< o }}{{ a := a + 1 }}{{< p }}{{ a := a + 1 }}{{< q }}{{ a := a + 1 }}{{< r }}{{ a := a + 1 }}{{< s }}{{ a := a + 1 }}{{< t }}{{ a := a + 1 }}{{< u }}{{ a := a + 1 }}{{< v }}{{ a := a + 1 }}{{< w }}{{ a := a + 1 }}{{< x }}{{ a := a + 1 }}{{< y }}{{ a := a + 1 }}{{< z }}{{ a := a + 1 }}{{ a }}{{/}}{{> z }}{{/}}{{> y }}{{/}}{{> x }}{{/}}{{> w }}{{/}}{{> v }}{{/}}{{> u }}{{/}}{{> t }}{{/}}{{> s }}{{/}}{{> r }}{{/}}{{> q }}{{/}}{{> p }}{{/}}{{> o }}{{/}}{{> n }}{{/}}{{> m }}{{/}}{{> l }}{{/}}{{> k }}{{/}}{{> j }}{{/}}{{> i }}{{/}}{{> h }}{{/}}{{> g }}{{/}}{{> f }}{{/}}{{> e }}{{/}}{{> d }}{{/}}{{> c }}{{/}}{{> b }}{{/}}{{> a }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("01", new TemplateLoader().load("Binding", "{{ a := 0 }}{{< a }}{{ a }}{{< b }}{{< c }}{{< d }}{{< e }}{{< f }}{{< g }}{{< h }}{{< i }}{{< j }}{{< k }}{{< l }}{{< m }}{{< n }}{{< o }}{{< p }}{{< q }}{{< r }}{{< s }}{{< t }}{{< u }}{{< v }}{{< w }}{{< x }}{{< y }}{{< z }}{{ a:= a + 1 }}{{ a }}{{/}}{{> z }}{{/}}{{> y }}{{/}}{{> x }}{{/}}{{> w }}{{/}}{{> v }}{{/}}{{> u }}{{/}}{{> t }}{{/}}{{> s }}{{/}}{{> r }}{{/}}{{> q }}{{/}}{{> p }}{{/}}{{> o }}{{/}}{{> n }}{{/}}{{> m }}{{/}}{{> l }}{{/}}{{> k }}{{/}}{{> j }}{{/}}{{> i }}{{/}}{{> h }}{{/}}{{> g }}{{/}}{{> f }}{{/}}{{> e }}{{/}}{{> d }}{{/}}{{> c }}{{/}}{{> b }}{{/}}{{> a }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testManyConsecutiveBindings() throws IOException, LoadException {
		assertEquals("1", new TemplateLoader().load("Binding", "{{ a := 0 }}{{< a }}{{ a := a + 1 }}{{/}}{{< b }}{{ a := a + 1 }}{{/}}{{< c }}{{ a := a + 1 }}{{/}}{{< d }}{{ a := a + 1 }}{{/}}{{< e }}{{ a := a + 1 }}{{/}}{{< f }}{{ a := a + 1 }}{{/}}{{< g }}{{ a := a + 1 }}{{/}}{{< h }}{{ a := a + 1 }}{{/}}{{< i }}{{ a := a + 1 }}{{/}}{{< j }}{{ a := a + 1 }}{{/}}{{< k }}{{ a := a + 1 }}{{/}}{{< l }}{{ a := a + 1 }}{{/}}{{< m }}{{ a := a + 1 }}{{/}}{{< n }}{{ a := a + 1 }}{{/}}{{< o }}{{ a := a + 1 }}{{/}}{{< p }}{{ a := a + 1 }}{{/}}{{< q }}{{ a := a + 1 }}{{/}}{{< r }}{{ a := a + 1 }}{{/}}{{< s }}{{ a := a + 1 }}{{/}}{{< t }}{{ a := a + 1 }}{{/}}{{< u }}{{ a := a + 1 }}{{/}}{{< v }}{{ a := a + 1 }}{{/}}{{< w }}{{ a := a + 1 }}{{/}}{{< x }}{{ a := a + 1 }}{{/}}{{< y }}{{ a := a + 1 }}{{/}}{{< z }}{{ a := a + 1 }}{{ a }}{{/}}{{> z }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testOverrideBinding() throws IOException, LoadException {
		assertEquals("1", new TemplateLoader().load("Binding Override", "{{ templateBinding := 0 }}{{ templateBinding := templateBinding + 1 }}{{ templateBinding }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testReadBinding() throws IOException, LoadException {
		assertEquals("2", new TemplateLoader().load("Read Binding", "{{ templateBinding := 1 }}{{< First }}{{ templateBinding + 1 }}{{/}}{{> First }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("1", new TemplateLoader().load("Read Binding", "{{ templateBinding := 1 }}{{< First }}{{ templateBinding := 2 }}{{/}}{{> First }}{{< Second }}{{ templateBinding }}{{/}}{{> Second }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testRecursiveBinding() throws IOException, LoadException {
		assertEquals("11", new TemplateLoader().load("Recursive Binding", "{{ templateBinding := 0 }}{{< First }}{{ templateBinding := templateBinding + 1 }}{{ templateBinding }}{{# a }}{{> First }}{{/}}{{/}}{{< Second }}{{ templateBinding := 1 }}{{> First }}{{/}}{{> Second }}").render(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("a", "b"), new java.io.StringWriter()).toString());
	}

	@Test
	void testSiblingBinding() throws IOException, LoadException {
		assertEquals("1", new TemplateLoader().load("Binding Sibling", "{{< First }}{{ templateBinding := 0 }}{{/}}{{< Second }}{{ templateBinding }}{{ templateBinding := 1 }}{{ templateBinding }}{{/}}{{> First }}{{> Second }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testStreamingReturnBinding() throws IOException, LoadException {
		assertEquals("1", new TemplateLoader().load("Binding", "{{ templateBinding := [1, 2, 3] #< i -> #^ i; null }}{{ templateBinding }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

}

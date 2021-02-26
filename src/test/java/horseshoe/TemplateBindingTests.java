package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;

import horseshoe.Settings.ContextAccess;

import org.junit.jupiter.api.Test;

class TemplateBindingTests {

	@Test
	void testBinding() throws IOException, LoadException {
		assertEquals("1", new TemplateLoader().load("Binding", "{{ templateBinding := 1 }}{{ templateBinding }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
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

}

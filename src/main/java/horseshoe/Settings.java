package horseshoe;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {

	public static interface EscapeFunction {
		/**
		 * Escapes the specified string.
		 *
		 * @param raw the raw string to escape
		 * @return the escaped string
		 */
		String escape(String raw);
	}

	public static final String DEFAULT_LINE_ENDING = System.lineSeparator();
	public static final String KEEP_TEMPLATE_LINE_ENDINGS = null;

	public static final EscapeFunction NO_ESCAPE_FUNCTION = null;

	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[&<>\"']");
	public static final EscapeFunction HTML_ESCAPE_FUNCTION = new EscapeFunction() {
		@Override
		public String escape(final String raw) {
			final StringBuilder sb = new StringBuilder();
			final Matcher matcher = ESCAPE_PATTERN.matcher(raw);
			int start = 0;

			while (matcher.find()) {
				final int found = matcher.start();
				sb.append(raw, start, found);

				switch (raw.charAt(found)) {
				case '&':  sb.append("&amp;");  break;
				case '<':  sb.append("&lt;");   break;
				case '>':  sb.append("&gt;");   break;
				case '"':  sb.append("&quot;"); break;
				case '\'': sb.append("&#39;");  break;
				default: throw new IllegalArgumentException("");
				}

				start = matcher.end();
			}

			return sb.append(raw, start, raw.length()).toString();
		}
	};

	/**
	 * Creates new mustache-compatible settings.
	 *
	 * @return new mustache-compatible settings
	 */
	public static Settings newMustacheSettings() {
		return new Settings().setAllowAccessToFullContextStack(true).setEscapeFunction(HTML_ESCAPE_FUNCTION).setLineEnding(KEEP_TEMPLATE_LINE_ENDINGS).setThrowOnTemplateNotFound(false);
	}

	private Charset charset = StandardCharsets.UTF_8;
	private EscapeFunction escapeFunction = NO_ESCAPE_FUNCTION;

	private boolean allowAccessToFullContextStack = false;
	private String lineEnding = DEFAULT_LINE_ENDING;
	private boolean throwOnTemplateNotFound = true;

	/**
	 * Gets whether or not expressions have full access to the settings stack during rendering.
	 *
	 * @return true if expressions have full access to the settings stack, otherwise false
	 */
	public boolean getAllowAccessToFullContextStack() {
		return allowAccessToFullContextStack;
	}

	/**
	 * Gets the character set used for loading templates.
	 *
	 * @return the character set used for loading templates
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Gets the escape function used by the rendering process.
	 *
	 * @return the escape function used by the rendering process. If null, the rendering process will not escape the text.
	 */
	public EscapeFunction getEscapeFunction() {
		return escapeFunction;
	}

	/**
	 * Gets the line ending used by the rendering process.
	 *
	 * @return the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 */
	public String getLineEnding() {
		return lineEnding;
	}

	/**
	 * Gets whether or not an exception will be thrown when a template is not found during loading.
	 *
	 * @return true if an exception will be thrown when a template is not found, otherwise false
	 */
	public boolean getThrowOnTemplateNotFound() {
		return throwOnTemplateNotFound;
	}

	/**
	 * Sets whether or not expressions have full access to the context stack during rendering.
	 *
	 * @param allowAccessToFullContextStack true to allow expressions have full access to the context stack, otherwise false
	 * @return this object
	 */
	public Settings setAllowAccessToFullContextStack(final boolean allowAccessToFullContextStack) {
		this.allowAccessToFullContextStack = allowAccessToFullContextStack;
		return this;
	}

	/**
	 * Sets the character set used for loading templates
	 *
	 * @param charset the character set used for loading templates
	 * @return this object
	 */
	public Settings setCharset(final Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Sets the escape function used by the rendering process.
	 *
	 * @param escapeFunction the escape function used by the rendering process. If null, the rendering process will not escape the text.
	 * @return this object
	 */
	public Settings setEscapeFunction(final EscapeFunction escapeFunction) {
		this.escapeFunction = escapeFunction;
		return this;
	}

	/**
	 * Sets the line ending used by the rendering process.
	 *
	 * @param lineEnding the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 * @return this object
	 */
	public Settings setLineEnding(final String lineEnding) {
		this.lineEnding = lineEnding;
		return this;
	}

	/**
	 * Sets whether or not an exception will be thrown when a template is not found during loading.
	 *
	 * @param throwOnTemplateNotFound true to throw an exception when a template is not found, otherwise false
	 * @return this object
	 */
	public Settings setThrowOnTemplateNotFound(final boolean throwOnTemplateNotFound) {
		this.throwOnTemplateNotFound = throwOnTemplateNotFound;
		return this;
	}

}

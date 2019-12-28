package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public interface AnnotationProcessor {

	/**
	 * Gets the writer based on the result of the expression.
	 *
	 * @param writer the writer from the enveloping section
	 * @param value the resulting map of the parameters for the annotation (if a single argument was given with no key, the key defaults to "value")
	 * @return the writer to use for the annotated section, or null to indicate that the writer should not be changed
	 */
	public Writer getWriter(final Writer writer, final Map<String, Object> value) throws IOException;

	/**
	 * Returns the writer. This method should close or flush the writer as appropriate.
	 *
	 * @param writer the writer to return
	 */
	public void returnWriter(final Writer writer) throws IOException;

}

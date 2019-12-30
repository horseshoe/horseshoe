package horseshoe;

import java.io.IOException;
import java.io.Writer;

public interface AnnotationHandler {

	/**
	 * Gets the writer based on the result of the expression.
	 *
	 * @param writer the writer from the enveloping section
	 * @param value the result of the expression for the annotation
	 * @return the writer to use for the annotated section, or null to indicate that the writer should not be changed
	 */
	public Writer getWriter(final Writer writer, final Object value) throws IOException;

}

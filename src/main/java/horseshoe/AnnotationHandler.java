package horseshoe;

import java.io.IOException;
import java.io.Writer;

/**
 * An {@link AnnotationHandler} is used to process annotations within {@link Template}s.
 * Annotations can be used to replace or supplement the writer used to render a section.
 */
public interface AnnotationHandler {

	/**
	 * Gets the writer based on the result of the expression.
	 *
	 * @param writer the writer from the enveloping section
	 * @param args the arguments passed to the annotation
	 * @return the writer to use for the annotated section, or null to indicate that the writer should not be changed
	 * @throws IOException if an I/O error occurs while manipulating the writer
	 */
	Writer getWriter(final Writer writer, final Object[] args) throws IOException;

}

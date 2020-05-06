package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.logging.Level;

import horseshoe.internal.Expression;
import horseshoe.internal.Properties;
import horseshoe.internal.RenderContext;

class SectionRenderer implements Action, Expression.Indexed {

	static final Factory FACTORY;

	protected final Section section;
	protected boolean hasNext;
	protected int index;

	public static class Factory {
		/**
		 * Creates a new section renderer using the specified section.
		 *
		 * @param section the section to be rendered
		 * @return the created section renderer
		 */
		SectionRenderer create(final Section section) {
			return new SectionRenderer(section);
		}
	}

	static {
		Factory factory = new Factory();

		try { // Try to load the Java 8+ version
			if (Properties.JAVA_VERSION >= 8.0) {
				factory = (Factory)Factory.class.getClassLoader().loadClass(Factory.class.getName().replace(SectionRenderer.class.getSimpleName(), SectionRenderer.class.getSimpleName() + "_8")).getConstructor().newInstance();
			}
		} catch (final ReflectiveOperationException e) {
			throw new ExceptionInInitializerError("Failed to load Java 8 specialization: " + e.getMessage());
		}

		FACTORY = factory;
	}

	/**
	 * Creates a new section renderer using the specified resolver and section.
	 *
	 * @param section the section to be rendered
	 */
	protected SectionRenderer(final Section section) {
		this.section = section;
	}

	/**
	 * Dispatches an array for rendering.
	 *
	 * @param context the render context
	 * @param data the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final Object data, final Writer writer) throws IOException {
		final Class<?> componentType = data.getClass().getComponentType();

		if (!componentType.isPrimitive()) {
			dispatchArray(context, (Object[])data, writer);
		} else if (int.class.equals(componentType)) {
			dispatchArray(context, (int[])data, writer);
		} else if (byte.class.equals(componentType)) {
			dispatchArray(context, (byte[])data, writer);
		} else if (double.class.equals(componentType)) {
			dispatchArray(context, (double[])data, writer);
		} else if (float.class.equals(componentType)) {
			dispatchArray(context, (float[])data, writer);
		} else if (char.class.equals(componentType)) {
			dispatchArray(context, (char[])data, writer);
		} else if (long.class.equals(componentType)) {
			dispatchArray(context, (long[])data, writer);
		} else if (boolean.class.equals(componentType)) {
			dispatchArray(context, (boolean[])data, writer);
		} else {
			dispatchArray(context, (short[])data, writer);
		}
	}

	/**
	 * Dispatches an object array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final Object[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches an int array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final int[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a byte array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final byte[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a double array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final double[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a float array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final float[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a char array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final char[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a long array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final long[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a boolean array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final boolean[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches a short array for rendering.
	 *
	 * @param context the render context
	 * @param array the array to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchArray(final RenderContext context, final short[] array, final Writer writer) throws IOException {
		if (array.length == 0) {
			renderInverted(context, writer);
		} else {
			context.getIndexedData().push(this);

			for (int i = 0; i < array.length; i++) {
				index = i;
				hasNext = i + 1 < array.length;
				render(context, array[index], writer);
			}

			context.getIndexedData().pop();
		}
	}

	/**
	 * Dispatches the data for rendering using the appropriate transformation for the specified data. Lists are iterated, booleans are evaluated, etc.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	protected void dispatchData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		if (data == null) {
			renderInverted(context, writer);
		} else if (data instanceof Iterable<?>) {
			dispatchIteratorData(context, ((Iterable<?>)data).iterator(), writer);
		} else if (data.getClass().isArray()) {
			dispatchArray(context, data, writer);
		} else if (!dispatchPrimitiveData(context, data, writer)) {
			render(context, data, writer);
		}

		if (section.cacheResult()) {
			context.setRepeatedSectionData(data);
		}
	}

	/**
	 * Renders the section using the specified writer and then closes it. The writer is guaranteed to be closed even if an exception is thrown. This method implements similar functionality to try-with-resources, but uses the exception to determine if we should log a failed close.
	 *
	 * @param context the render context
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void renderAndCloseWriter(final RenderContext context, final Writer writer) throws IOException {
		Exception renderException = null;

		try {
			renderWithoutData(context, writer);
		} catch (final IOException e) {
			renderException = e;
			throw e;
		} finally {
			try {
				writer.close();
			} catch (final IOException e) {
				if (renderException == null) { // Log only when there is no causing exception to prevent confusion
					context.getSettings().getLogger().log(Level.WARNING, "Failed to close writer for section \"" + section.getName() + "\" (" + section.getLocation() + ")", e);
				} else { // If there is a causing exception, add this one as a suppressed exception
					renderException.addSuppressed(e);
				}
			}
		}
	}

	/**
	 * Dispatches iterator data for rendering.
	 *
	 * @param context the render context
	 * @param it the iterator to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	protected final void dispatchIteratorData(final RenderContext context, final Iterator<?> it, final Writer writer) throws IOException {
		if (!it.hasNext()) {
			renderInverted(context, writer);
			return;
		}

		context.getIndexedData().push(this);

		for (hasNext = true, index = 0; true; index++) {
			final Object object = it.next();

			if (!it.hasNext()) {
				hasNext = false;
				render(context, object, writer);
				break;
			}

			render(context, object, writer);
		}

		context.getIndexedData().pop();
	}

	/**
	 * Attempts to dispatch primitive data for rendering.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @return true if the data was dispatched as a primitive data type, otherwise false
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private boolean dispatchPrimitiveData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		if (data instanceof Boolean) {
			if (((Boolean)data).booleanValue()) {
				renderWithoutData(context, writer);
			} else {
				renderInverted(context, writer);
			}

			return true;
		}

		final boolean isZero;

		if (data instanceof Number) {
			if (data instanceof Double || data instanceof Float) {
				isZero = ((Number)data).doubleValue() == 0.0;
			} else if (data instanceof BigDecimal) {
				isZero = BigDecimal.ZERO.compareTo((BigDecimal)data) == 0;
			} else if (data instanceof BigInteger) {
				isZero = BigInteger.ZERO.equals(data);
			} else {
				isZero = ((Number)data).longValue() == 0;
			}
		} else if (data instanceof Character) {
			isZero = (((Character)data).charValue() == 0);
		} else {
			return false;
		}

		if (isZero) {
			renderInverted(context, writer);
		} else {
			render(context, data, writer);
		}

		return true;
	}

	@Override
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the section that is rendered by this renderer.
	 *
	 * @return the section that is rendered by this renderer
	 */
	Section getSection() {
		return section;
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public final void perform(final RenderContext context, final Writer writer) throws IOException {
		final int contextSize = context.getSectionData().size();

		try {
			if (section.getAnnotation() == null) { // Dispatch the data as normal, if not an annotation
				dispatchData(context, section.useCache() ? context.getRepeatedSectionData() : section.getExpression().evaluate(context), writer);
				return;
			}

			final AnnotationHandler annotationProcessor = context.getAnnotationMap().get(section.getAnnotation());

			if (annotationProcessor == null) { // Render inverted actions if the annotation processor is not available
				renderInverted(context, writer);
				return;
			}

			final Writer annotationWriter = annotationProcessor.getWriter(writer, section.getExpression() == null ? null : section.getExpression().evaluate(context));

			if (annotationWriter == null || annotationWriter == writer) { // If the writer is not changed then render the actions using the current writer
				renderWithoutData(context, writer);
			} else { // Otherwise, render the actions using the new writer and close it when finished
				renderAndCloseWriter(context, annotationWriter);
			}
		} catch (final IOException e) { // Bubble up the exception, only logging at the lowest level
			if (contextSize + (section.isInvisible() ? 0 : 1) == context.getSectionData().size()) {
				context.getSettings().getLogger().log(Level.SEVERE, "Encountered exception while rendering section \"" + section.getName() + "\" (" + section.getLocation() + ")", e);
			}

			throw e;
		}
	}

	/**
	 * Renders the section using the specified context, data, and writer.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void render(final RenderContext context, final Object data, final Writer writer) throws IOException {
		context.getSectionData().push(data);
		renderWithoutData(context, writer);
		context.getSectionData().pop();
	}

	/**
	 * Renders the section using the specified context and writer.
	 *
	 * @param context the render context
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void renderWithoutData(final RenderContext context, final Writer writer) throws IOException {
		for (final Action action : section.getActions()) {
			action.perform(context, writer);
		}
	}

	/**
	 * Renders the inverted section using the specified context and writer.
	 *
	 * @param context the render context
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void renderInverted(final RenderContext context, final Writer writer) throws IOException {
		for (final Action action : section.getInvertedActions()) {
			action.perform(context, writer);
		}
	}

}

package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

public class SectionRenderer implements Renderer {

	private final Section section;

	static class Reiterable<T> implements Iterable<T>, Iterator<T> {
		private final ArrayList<T> reiterableList = new ArrayList<>();
		private final Iterator<T> iterator;

		public Reiterable(final Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			final T item = iterator.next();

			reiterableList.add(item);
			return item;
		}

		@Override
		public void remove() {
			iterator.remove();
			reiterableList.remove(reiterableList.size() - 1);
		}

		@Override
		public Iterator<T> iterator() {
			return reiterableList.iterator();
		}
	}

	/**
	 * Creates a new section renderer using the specified resolver and section.
	 *
	 * @param section the section to be rendered
	 */
	SectionRenderer(final Section section) {
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
			final SectionRenderData renderData = new SectionRenderData();
			context.getSectionData().push(renderData);

			for (int i = 0; i < array.length; i++) {
				renderData.update(array[i], i, i + 1 < array.length);
				renderSection(context, writer);
			}

			context.getSectionData().pop();
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
	void dispatchData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		Object unwrappedData;

		if (data instanceof Optional<?>) {
			unwrappedData = ((Optional<?>) data).orElse(null);
		} else {
			unwrappedData = data;
		}

		if (unwrappedData instanceof Stream<?>) {
			unwrappedData = ((Stream<?>) unwrappedData).iterator();
		}

		if (unwrappedData == null) {
			renderInverted(context, writer);
		} else if (unwrappedData instanceof Iterable) {
			if (section.cacheResult() && !(unwrappedData instanceof Collection)) {
				unwrappedData = new Reiterable<>(((Iterable<?>) unwrappedData).iterator());
				dispatchIteratorData(context, (Iterator<?>) unwrappedData, writer);
			} else {
				dispatchIteratorData(context, ((Iterable<?>) unwrappedData).iterator(), writer);
			}
		} else if (unwrappedData instanceof Iterator) {
			if (section.cacheResult()) {
				unwrappedData = new Reiterable<>((Iterator<?>) unwrappedData);
			}

			dispatchIteratorData(context, (Iterator<?>) unwrappedData, writer);
		} else if (unwrappedData.getClass().isArray()) {
			dispatchArray(context, unwrappedData, writer);
		} else if (!dispatchPrimitiveData(context, unwrappedData, writer)) {
			context.getSectionData().push(new SectionRenderData(unwrappedData));
			renderSection(context, writer);
			context.getSectionData().pop();
		}

		if (section.cacheResult()) {
			context.setRepeatedSectionData(unwrappedData);
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
	final void dispatchIteratorData(final RenderContext context, final Iterator<?> it, final Writer writer) throws IOException {
		if (!it.hasNext()) {
			renderInverted(context, writer);
			return;
		}

		final SectionRenderData renderData = new SectionRenderData();
		context.getSectionData().push(renderData);

		for (renderData.hasNext = true; true; renderData.index++) {
			renderData.data = it.next();

			if (!it.hasNext()) {
				renderData.hasNext = false;
				renderSection(context, writer);
				break;
			}

			renderSection(context, writer);
		}

		context.getSectionData().pop();
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
				renderSection(context, writer);
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
			context.getSectionData().push(new SectionRenderData(data));
			renderSection(context, writer);
			context.getSectionData().pop();
		}

		return true;
	}

	@Override
	public final void render(final RenderContext context, final Writer writer) throws IOException {
		final int contextSize = context.getSectionData().size();

		try {
			if (section.getAnnotation() == null) { // Dispatch the data as normal, if not an annotation
				dispatchData(context, section.useCache() ? context.getRepeatedSectionData() : section.getExpression().evaluate(context), writer);
				return;
			}

			final AnnotationHandler annotationProcessor = context.getAnnotationMap().get(section.getAnnotation());
			Object[] args = null;

			// Render inverted actions if the annotation processor is not available or the expression evaluates to null
			if (annotationProcessor == null ||
					(section.getExpression() != null && (args = (Object[]) section.getExpression().evaluate(context)) == null)) {
				renderInverted(context, writer);
				return;
			}

			final Writer annotationWriter;

			try {
				annotationWriter = annotationProcessor.getWriter(writer, args);
			} catch (final Exception e) {
				context.getSettings().getLogger().log(Level.WARNING, "Encountered exception with annotation section " + section, e);
				renderInverted(context, writer);
				return;
			}

			if (annotationWriter == null || annotationWriter == writer) { // If the writer is not changed then render the actions using the current writer
				renderSection(context, writer);
			} else { // Otherwise, render the actions using the new writer and close it when finished
				renderAndCloseWriter(context, annotationWriter);
			}
		} catch (final IOException e) { // Bubble up the exception, only logging at the lowest level
			if (contextSize + (section.isInvisible() ? 0 : 1) == context.getSectionData().size()) {
				context.getSettings().getLogger().log(Level.SEVERE, "Encountered exception while rendering section " + section, e);
			}

			throw e;
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
			renderSection(context, writer);
		} catch (final IOException e) {
			renderException = e;
			throw e;
		} finally {
			try {
				writer.close();
			} catch (final IOException e) {
				if (renderException == null) { // Log only when there is no causing exception to prevent confusion
					context.getSettings().getLogger().log(Level.WARNING, "Failed to close writer for section " + section, e);
				} else { // If there is a causing exception, add this one as a suppressed exception
					renderException.addSuppressed(e);
				}
			}
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
		for (final Renderer action : section.getInvertedRenderList()) {
			action.render(context, writer);
		}
	}

	/**
	 * Renders the section using the specified context and writer.
	 *
	 * @param context the render context
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void renderSection(final RenderContext context, final Writer writer) throws IOException {
		for (final Renderer action : section.getRenderList()) {
			action.render(context, writer);
		}
	}

}

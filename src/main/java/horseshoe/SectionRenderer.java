package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
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
		SectionRenderer create(final Section section){
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
			Template.LOGGER.log(Level.WARNING, "Failed to load class: {0}, falling back to default", e.getMessage());
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
		final int length = Array.getLength(data);
		index = 0;

		if (length == 0) {
			hasNext = false;
			renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			return;
		}

		final Class<?> componentType = data.getClass().getComponentType();
		context.getIndexedData().push(this);

		if (!componentType.isPrimitive()) {
			for (final Object[] array = (Object[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (int.class.equals(componentType)) {
			for (final int[] array = (int[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (byte.class.equals(componentType)) {
			for (final byte[] array = (byte[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (double.class.equals(componentType)) {
			for (final double[] array = (double[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (boolean.class.equals(componentType)) {
			for (final boolean[] array = (boolean[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (float.class.equals(componentType)) {
			for (final float[] array = (float[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (long.class.equals(componentType)) {
			for (final long[] array = (long[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else if (char.class.equals(componentType)) {
			for (final char[] array = (char[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		} else {
			for (final short[] array = (short[])data; index < length; index++) {
				hasNext = index + 1 < length;
				renderActions(context, array[index], writer, section.getActions());
			}
		}

		context.getIndexedData().pop();
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
			renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
		} else if (data instanceof Iterable<?>) {
			dispatchIteratorData(context, ((Iterable<?>)data).iterator(), writer);
		} else if (data.getClass().isArray()) {
			dispatchArray(context, data, writer);
		} else if (data instanceof Boolean) {
			if ((Boolean)data) {
				renderActions(context, context.getSectionData().peek(), writer, section.getActions());
			} else {
				renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			}
		} else {
			renderActions(context, data, writer, section.getActions());
		}

		if (section.cacheResult()) {
			context.setRepeatedSectionData(data);
		}
	}

	/**
	 * Dispatches the data for rendering using the appropriate transformation for the specified data. Lists are iterated, booleans are evaluated, etc. The writer is guaranteed to be closed when this method finishes or an exception is thrown. This method implements similar functionality to try-with-resources, but uses the exception to determine if we should log a failed close.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void dispatchDataAndCloseWriter(final RenderContext context, final Object data, final Writer writer) throws IOException {
		Exception dispatchException = null;

		try {
			dispatchData(context, data, writer);
		} catch (final IOException e) {
			dispatchException = e;
			throw e;
		} finally {
			try {
				writer.close();
			} catch (final IOException e) {
				if (dispatchException == null) { // Log only when there is no causing exception to prevent confusion
					context.getSettings().getLogger().log(Level.WARNING, "Failed to close writer (" + section.getLocation() + ")", e);
				} else { // If there is a causing exception, add this one as a suppressed exception
					dispatchException.addSuppressed(e);
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
		hasNext = it.hasNext();
		index = 0;

		if (!hasNext) {
			renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			return;
		}

		context.getIndexedData().push(this);

		while (true) {
			final Object object = it.next();

			if (it.hasNext()) {
				renderActions(context, object, writer, section.getActions());
			} else {
				hasNext = false;
				renderActions(context, object, writer, section.getActions());
				break;
			}

			index++;
		}

		context.getIndexedData().pop();
	}

	@Override
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the section that is rendered by this action
	 *
	 * @return the section that is rendered by this action
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
		try {
			if (section.getAnnotation() == null) {
				dispatchData(context, section.useCache() ? context.getRepeatedSectionData() : section.getExpression().evaluate(context), writer);
			} else {
				final AnnotationHandler annotationProcessor = context.getAnnotationMap().get(section.getAnnotation());

				if (annotationProcessor == null) { // Only write the data if there is an annotation processor available, otherwise take false path with default writer
					dispatchData(context, false, writer);
				} else {
					final Writer annotationWriter = annotationProcessor.getWriter(writer, section.getExpression() == null ? null : section.getExpression().evaluate(context));

					if (annotationWriter == null || annotationWriter == writer) { // If we are not using a new writer, then don't close it
						dispatchData(context, context.getSectionData().peek(), writer);
					} else {
						dispatchDataAndCloseWriter(context, context.getSectionData().peek(), annotationWriter);
					}
				}
			}
		} catch (final IOException e) {
			context.getSettings().getLogger().log(Level.SEVERE, "Failed to render section (" + section.getLocation() + ")", e);
			throw e;
		}
	}

	/**
	 * Renders the actions using the specified context, data, and writer.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @param actions the actions to render
	 * @throws IOException if an error occurs while writing to the writer
	 */
	protected final void renderActions(final RenderContext context, final Object data, final Writer writer, final List<Action> actions) throws IOException {
		if (section.isInvisible()) {
			for (final Action action : actions) {
				action.perform(context, writer);
			}
		} else {
			context.getSectionData().push(data);

			for (final Action action : actions) {
				action.perform(context, writer);
			}

			context.getSectionData().pop();
		}
	}

}

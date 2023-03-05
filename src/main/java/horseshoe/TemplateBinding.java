package horseshoe;

final class TemplateBinding {

	private final String name;
	private final int templateIndex;
	private final int index;

	/**
	 * Creates a new template binding.
	 *
	 * @param name the name of the template binding
	 * @param templateIndex the template index of the template binding
	 * @param index the index of the template binding within the template
	 */
	TemplateBinding(final String name, final int templateIndex, final int index) {
		this.name = name;
		this.templateIndex = templateIndex;
		this.index = index;
	}

	/**
	 * Gets the name of the template binding.
	 *
	 * @return the name of the template binding
	 */
	String getName() {
		return name;
	}

	/**
	 * Gets the index of the template binding.
	 *
	 * @return the index of the template binding
	 */
	int getIndex() {
		return index;
	}

	/**
	 * Gets the template index for the template binding.
	 *
	 * @return the template index for the template binding
	 */
	int getTemplateIndex() {
		return templateIndex;
	}

}

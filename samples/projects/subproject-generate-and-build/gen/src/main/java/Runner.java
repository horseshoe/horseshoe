import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import horseshoe.*;

public class Runner {

	public static void main(final String[] args) throws LoadException, IOException {
		final Settings settings = new Settings();
		final TemplateLoader loader = new TemplateLoader(Arrays.asList(Paths.get(System.getProperty("templateRootDir"))));
		final Map<String, AnnotationHandler> annotationHandlers = new HashMap<>(AnnotationHandlers.DEFAULT_ANNOTATIONS);
		annotationHandlers.replace("File", AnnotationHandlers.fileWriter(Paths.get(System.getProperty("templateOutputDir")), StandardCharsets.UTF_8));
		for (final String templateFileName : System.getProperty("templateFileNames").split(",")) {
			final Template template = loader.load(Paths.get(templateFileName));
			template.render(settings, Collections.singletonMap("values", new int[] { 1, 2, 3 }), new PrintWriter(System.out), annotationHandlers);
		}
	}

}

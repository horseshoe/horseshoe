package example;

import horseshoe.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Runner {

	static final Path outputFile = Paths.get("build/generated/HelloWorld.txt");

	public static void main(final String[] args) {
		final Settings settings = new Settings();
		final TemplateLoader loader = new TemplateLoader();
		final Template t = loader.load(Paths.get("../../data/HelloWorld.U"));
		try (final FileWriter writer = new FileWriter(outputFile.toFile())) {
			t.render(settings, Collections.<String, Object>emptyMap(), writer);
		}
	}

}
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

public class MustacheTestImporter {

	private static String capitalize(final String value) {
		return value.substring(0, 1).toUpperCase() + value.substring(1);
	}

	private static String escapeCodeString(final String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
	}

	@SuppressWarnings("unchecked")
	private static StringBuilder loadListString(final StringBuilder sb, final List<Object> list) {
		if (list != null) {
			String start = "";

			for (final Object obj : list) {
				sb.append(start);

				if (obj instanceof Boolean) {
					sb.append(obj);
				} else if (obj instanceof Map) {
					sb.append("Helper.loadMap(");
					loadMapString(sb, (Map<String, Object>)obj);
					sb.append(')');
				} else if (obj instanceof List) {
					sb.append("Helper.loadList(");
					loadListString(sb, (List<Object>)obj);
					sb.append(')');
				} else if (obj != null) {
					sb.append('"').append(escapeCodeString(obj.toString())).append('"');
				} else {
					sb.append("null");
				}

				start = ", ";
			}
		}

		return sb;
	}

	@SuppressWarnings("unchecked")
	private static StringBuilder loadMapString(final StringBuilder sb, final Map<String, Object> map) {
		if (map != null) {
			String start = "\"";

			for (final Entry<String, Object> entry : map.entrySet()) {
				sb.append(start).append(escapeCodeString(entry.getKey())).append("\", ");

				if (entry.getValue() instanceof Boolean) {
					sb.append(entry.getValue());
				} else if (entry.getValue() instanceof Map) {
					sb.append("Helper.loadMap(");
					loadMapString(sb, (Map<String, Object>)entry.getValue());
					sb.append(')');
				} else if (entry.getValue() instanceof List) {
					sb.append("Helper.loadList(");
					loadListString(sb, (List<Object>)entry.getValue());
					sb.append(')');
				} else if (entry.getValue() != null) {
					sb.append('"').append(escapeCodeString(entry.getValue().toString())).append('"');
				} else {
					sb.append("null");
				}

				start = ", \"";
			}
		}

		return sb;
	}

	@SuppressWarnings("unchecked")
	public static void main(final String[] args) throws IOException {
		final Path destination = Paths.get(args[1]);

		Files.newDirectoryStream(Paths.get(args[0]), path -> !path.getFileName().toString().startsWith("~") && path.toString().endsWith(".yml")).
			forEach(path -> {
				final String filename = path.getFileName().toString();
				final String className = capitalize((filename.startsWith("~") ? filename.substring(1) : filename).replace(".yml", ""));
				final Yaml yaml = new Yaml();

				System.out.println("Loading " + path.toString() + "...");

				try (final InputStream in = new FileInputStream(path.toString());
						final PrintWriter out = new PrintWriter(new FileWriter(destination.resolve(className + ".java").toString()))) {
					out.print("package horseshoe.mustache;" + System.lineSeparator() +
							System.lineSeparator() +
							"import org.junit.Test;" + System.lineSeparator() +
							System.lineSeparator() +
							"import horseshoe.Helper;" + System.lineSeparator() +
							System.lineSeparator() +
							"public class " + className + " {" + System.lineSeparator() +
							System.lineSeparator() +
							"	@Test" + System.lineSeparator() +
							"	public void test() throws horseshoe.LoadException, java.io.IOException {");

					for (final Object obj : (List<Object>)((Map<String, Object>)yaml.load(in)).get("tests")) {
						final Map<String, Object> test = (Map<String, Object>)obj;

						out.println(System.lineSeparator() + "\t\t/* " + test.get("name").toString() + " - " + test.get("desc").toString() + " */");
						out.println("\t\tHelper.executeMustacheTest(\"" + escapeCodeString(test.get("template").toString()) + "\", Helper.loadMap(" + loadMapString(new StringBuilder(), (Map<String, Object>)test.get("data")) + "), Helper.loadMap(" + loadMapString(new StringBuilder(), (Map<String, Object>)test.get("partials")) + "), \"" + escapeCodeString(test.get("expected").toString()) + "\");");
					}

					out.println("	}" + System.lineSeparator() +
							System.lineSeparator() +
							"}");
				} catch (final IOException e) {
					System.err.println("Failed to load " + path.toString() + ": " + e.getMessage());
				}

				System.out.println(" Done.");
			});
	}

}

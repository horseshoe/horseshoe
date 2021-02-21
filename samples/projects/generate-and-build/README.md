# Example Gradle Generate and Build Project

Sample project that creates Java source code files from
`src/main/horseshoe/Template.U` with the `generateCode` task,
compiles the Java source using the `compileJava` Gradle task,
and then executes the compiled Java class files in the `run` task.

The expected output of this project is the printing of `"Hello world!"`
to stdout. This is executed within a single Gradle project and does
not require the use of Gradle subprojects.

This example can be replicated when the data consumed by the templates
can be expressed by static data as consumed by the Horseshoe template
engine, either by a key-value pair as a command-line argument
(`-Dkey=value`/`--define=key=value`), or as a JSON data file
(`-d`/`--data-file`).
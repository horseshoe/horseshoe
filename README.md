## Note: This is a pre-release version, use at your own risk

# Horseshoe
Horseshoe for Java

![Build](https://github.com/nicklauslittle/horseshoe-j/workflows/Build/badge.svg)
[![Codacy](https://api.codacy.com/project/badge/Grade/07cec89cb05f4ed4ba8759f6ad8bdc97)](https://www.codacy.com/manual/nicklaus.little/horseshoe-j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=nicklauslittle/horseshoe-j&amp;utm_campaign=Badge_Grade)
[![Coverage](https://api.codacy.com/project/badge/Coverage/07cec89cb05f4ed4ba8759f6ad8bdc97)](https://www.codacy.com/manual/nicklaus.little/horseshoe-j?utm_source=github.com&utm_medium=referral&utm_content=nicklauslittle/horseshoe-j&utm_campaign=Badge_Coverage)
[![Coverity](https://scan.coverity.com/projects/20222/badge.svg)](https://scan.coverity.com/projects/nicklauslittle-horseshoe-j)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=nicklauslittle_horseshoe-j&metric=security_rating)](https://sonarcloud.io/dashboard?id=nicklauslittle_horseshoe-j)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=nicklauslittle_horseshoe-j&metric=bugs)](https://sonarcloud.io/dashboard?id=nicklauslittle_horseshoe-j)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=nicklauslittle_horseshoe-j&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=nicklauslittle_horseshoe-j)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=nicklauslittle_horseshoe-j&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=nicklauslittle_horseshoe-j)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nicklauslittle_horseshoe-j&metric=alert_status)](https://sonarcloud.io/dashboard?id=nicklauslittle_horseshoe-j)
[![license](https://img.shields.io/github/license/nicklauslittle/horseshoe-j?label=License&logo=github)](https://github.com/nicklauslittle/horseshoe-j/blob/master/LICENSE)

## Goals
- Provide a Mustache-like template engine for generating source code.
- Allow both static and dynamically-generated data models.
- Allow common operator symbols to be used to support basic expressions and manipulations.
- Allow iteration over iterable objects with support for basic properties such as first/last/index.
- Allow iterations to be easily repeatable.

## FAQ
### What is Horseshoe?
Horseshoe is a templating system used to generate information from a data model. It was primarily designed around the use case of quickly generating source code for languages like C++ and Java. It is written in Java with an emphasis on minimizing dependencies. It uses a Mustache-like syntax with an extended expression system supporting method calls to allow processing of both static data and dynamically-generated data.

### How is Horseshoe similar to Mustache?
Horseshoe uses the same tags as Mustache. These tags have the same meaning as they do in Mustache. Due to the extended expression syntax, it is difficult to gauge whether or not it is Mustache-compliant. However, when a Mustache-compliant context object is used, Horseshoe passes all required tests in the Mustache specification v1.1.2.

### How is Horseshoe different from Mustache?
Horseshoe does not have the same design goals as Mustache, resulting in many different decisions when creating the specification. For example, Horseshoe was designed for generation of source code, not HTML. For this reason, content is not HTML-escaped by default (the settings can be created with `horseshoe.Settings.newMustacheSettings()` to support this feature). Other differences include a more complex expression syntax ("interpolation" in Mustache-speak) and support for method calls in expressions.

### Does Horseshoe support Mustache lambdas?
Horseshoe does not support Mustache lambdas. It foregoes lambdas in favor of an expression syntax that supports method calls. Expressions can be named and reused to support similar functionality to Mustache lambdas.

### What literal types are supported in Horseshoe expressions?
The following table summarizes the literals supported by Horseshoe. A more detailed explanation of each can be found in the [Expressions](#expressions) section.

| Java Type | Horseshoe Literal |
|-----------|-------------------|
| `int` | `123` |
| `long` | `123L` / `0x100000000` |
| `double` | `3.14` |
| `boolean` | `true` / `false` |
| `java.lang.Object` | `null` |
| `java.lang.String` | `"a string"` |
| `java.util.regex.Pattern` | `~/pattern/` |
| `java.util.List` | `[1,2,3,4]` |
| `java.util.Map` | `[1:2,3:4]` / `[:]` |

### Which operators are supported by Horseshoe expressions?
Horseshoe supports most non-assignment operators used in common languages. The equals operator can be used to bind a name to a local statement. Note that a bound name is scoped to a single expression and is not the same as an assignment operator. For more information on all the operators, see the [Supported Operators](#supported-operators) section.

### What extension should be used for Horseshoe template files?
Horseshoe supports any file extension for template files. However, convention is to use a capital "U", which resembles a horseshoe.

### How is whitespace within a template handled by Horseshoe?
Horseshoe uses the same whitespace paradigm as Mustache. All tags except for content (and unescaped content) and partial tags qualify for consideration as standalone tags. In this case, the entire line is excluded from output and only processed by Horseshoe. More details can be found in the [Tags](#tags) section.

## Code Example
The following example demonstrates how to use the Horseshoe library from within Java code. [Template examples](#template-examples) are located in a separate section.

First, a template is loaded using the template loader class. Templates can be loaded from a string, a file, or a reader.
```java
final horseshoe.Template template = new horseshoe.TemplateLoader().load("Hello World", "{{ salutation }}, {{ audience }}!");
// final horseshoe.Template mustacheTemplate = horseshoe.TemplateLoader.newMustacheLoader().load("Hello World", "{{ salutation }}, {{ audience }}!");
```

Next, a data map is created that contains all the data used to render the template.
```java
final java.util.Map<String, Object> data = new java.util.HashMap<>();
data.put("salutation", "Hello");
data.put("audience", "world");
```

Finally, the template is rendered to a writer using the settings and the data.
```java
final horseshoe.Settings settings = new horseshoe.Settings();
// final horseshoe.Settings mustacheSettings = horseshoe.Settings.newMustacheSettings();
final java.io.StringWriter writer = new java.io.StringWriter();

template.render(settings, data, writer);
System.out.println(writer.toString()); // Prints "Hello, world!"
```

## Template Examples
Examples of Horseshoe templates can be found in the `samples` directory. The `samples/results` directory contains the results of running these templates through the Horseshoe engine runner with default settings and an empty data map. (Line endings may vary, as they are operating system dependent by default.)

The Horseshoe engine runner can be used to render templates from the command line. The runner can be invoked using Java's `-jar` argument: `java -jar horseshoe.jar`. It provides a subset of the rendering options available in the Horseshoe library. A list of all available options for the runner can be listed using the `--help` argument.

## Description
Horseshoe is a Mustache-like templating system focused on fast source code generation. The templates consist of mixed tags and content that are ingested and rendered in accordance with a data model. Horseshoe uses an advanced expression syntax to allow data-driven rendering.

### Tags
Horseshoe uses tags to specify dynamic parts of a template. The tags typically start with `{{` and end with `}}`. (The delimiters can be changed using the set delimiter tag.) Those familiar with Mustache will recognize the majority of the tags used by Horseshoe.

All tags except for content tags may qualify as stand-alone tags. A stand-alone tag is a tag that contains only whitespace before the opening braces on the beginning line and only whitespace after the closing braces on the ending line. This holds true for multiline tags and when using custom delimiters.

#### Comments
Comment tags (`{{! ignore this }}`) are structured the same as Mustache comments. A comment tag beings with `!`.

#### Content
Content tags (`{{ content }}`) are similar to Mustache interpolation tags. The current context is used to look up the value specified inside the tag. The value is then rendered in place of the tag.

There are a couple major differences from Mustache interpolation tags:
1. The printed values are not HTML-escaped by default, since Horseshoe is designed for generating source code rather than HTML. Horseshoe can be configured to escape HTML if desired.
2. The value specified inside the content tag represents a [Horseshoe expression](#expressions). Values such as `some-content` will be interpreted as the value `some` minus the value `content` rather than the value of `some-content`. This can be changed via a setting to match the Mustache interpolation tag if desired.

#### Unescaped Content
Unescaped content (`{{{ content }}}`, `{{& content }}`) is the same as normal content in Horseshoe, since content is not escaped by default. It only differs from normal content if content escaping is enabled. The tag can either start with a `{` and end with a `}` or simply start with a `&`.

#### Partials
Partial tags (`{{> partial }}`) are similar to partial tags in Mustache. They function similarly to a `#include` directive in C++. The partial template is loaded (either from the specified filename or the corresponding template from the template loader) and placed into the current template.

If a partial tag is a stand-alone tag, the indentation of the partial tag will be prepended to every line of the partial template. The double indirection operator can be used on partial tags (`{{>> partial }}`) to avoid applying the indentation to every line yet still ignore trailing whitespace and newline. This is a Horseshoe feature.

#### Set Delimiter (`{{=<% %>=}}`)
The set delimiter tag is structured the same as it is for Mustache. It is used to change the delimiters from `{{` and `}}` to other sequences in templates that contain many double braces that are part of the literal text. The new sequences are applicable for all tags. Here is a brief example,
```horseshoe
{{= << >> =}}
{{ Literal template text. }}
<<! This is a comment and the line below is an unescaped content tag. >>
<<{ content }>>
More {{ literal }} template text.
<<! Return to using `{{` and `}}`. >>
<<={{ }}=>>
```

#### Sections
Section tags (`{{# map }}`) in Horseshoe are similar to Mustache section tags. A section tag can either function as a conditional section or as a foreach section. The contents of a conditional section are only rendered if the value evaluates to a non-null, non-zero value. The contents of a foreach section are iterated once for every child.

Internal objects that are treated as foreach sections include arrays, iterables, and streams (if using Java 8 and up). All other objects are treated as conditional sections. For booleans, the conditional section will only be rendered for `true` values. For numeric or character primitives, the conditional section will only be rendered for non-zero values. For optionals (if using Java 8 and up), the conditional section will only be rendered for values that are present. The conditional section is never rendered for a null value.

All sections except for booleans and [annotations](#annotations) push the value onto the context stack. Foreach sections push each child item onto the stack as it iterates over the collection.

#### Repeated Sections
Repeated section tags (`{{#}}`) are used to duplicate the previous section at the same scope. Nested scopes within a repeated section can also be repeated, allowing a hierarchical repeating of sections. For example,
```horseshoe
<h1>Contents</h1>
{{# ['Feature 1', 'Feature 2', 'Feature 3'] }}
	<ul><li><a href="#{{ replace(' ', '') }}">{{.}}</a><ul>
	{{# ['x86', 'x86-64', 'ArmHf', 'Aarch64'] }}
		<li><a href="#{{ ../replace(' ', '') }}_{{ replace(' ', '') }}">{{.}}</a></li>
	{{/}}
	</ul></li></ul>
{{/}}
{{#}}
<h1 id="{{ replace(' ', '') }}">{{.}}</h1>
	{{#}}
	<h2 id="{{ ../replace(' ', '') }}_{{ replace(' ', '') }}">{{.}}</h2>
	{{/}}
{{/}}
```

#### Annotations
Annotations (`{{# @Annotation("Param1": false, "Param2") }}`) are section tags that begin with an at sign (`@`). The parameters are parsed as a [Horseshoe expression](#expressions) and passed to the annotation handler. Annotations do not affect the context stack and are not considered in scope for repeated sections.

Built-in annotations include the following:
- @StdErr - Sends output to stderr.
- @StdOut - Sends output to stdout.
- @File('name': 'filename', 'encoding': 'UTF-16', 'overwrite': false, 'append') - Sends output to the file with the specified name, using the specified encoding. The file is only written if the contents of the file are different than the rendered contents, unless overwrite is specified as true. If append is specified as true the rendered contents are appended to the file.

#### Inverted Sections
Inverted section tags (`{{^ exists }}`) are used to negate a conditional or foreach section. Inverted sections are only rendered when the negated section would not be rendered. Null objects, numeric zero values, `false`, as well as empty lists or arrays are several examples of values that will cause an inverted section to be rendered.

An inverted section tag can be left empty (`{{^}}`) to indicate that the inverted section will be rendered when the conditional associated with the current section is false. For example,
```horseshoe
{{# a }}
  a evaluates to true
{{^}}
  a evaluates to false
{{/ a }}
```

#### Inline Partials
Inline partial tags (`{{< partial }}`) define a partial template inline in the current template. In this way, partial templates can be nested instead of requiring them to be loaded from another source, like a string or template file.

Inline partials can only be included (using a partial tag) in the scope of the template in which they are declared. They cannot be included in any other template. This prevents naming collisions of inline partials in multiple templates and allows inline partials to be overridden later in a template or in a nested scope.

### Expressions
Horseshoe expressions look a lot like expressions in modern programming languages. Expressions can contain named values, literals, method calls, and operator symbols. Expressions can also contain comments using C, C++, and Java style comments using either `/*...*/` or `//...`. Multiple lines are allowed inside a single expression.

Multiple statements can be chained together inside an expression, using a semicolon (`;`) as a separator. A trailing semicolon at the end of the expression is not needed. A comma (`,`) is used to separate method parameters and list or map entries.

Horseshoe expressions use `[]` or `{}` for both list and map literals. Any list that contains a colon (`:`) separator is treated as a map. Entries in a map literal without a colon are treated as if the given item is used as both the key and the value. The `{}` is used for creating iterable maps, whereas the `[]` maps can be used to lookup values. The `[]` should be used anywhere iteration is not desired.

Commas can be used anywhere within an expression. If a comma is used in a context where it would otherwise not be allowed (`{{ 4, 5 }}`), the result is interpreted as if it were wrapped in `{}` to form a list or iterable map. These are considered auto-converted lists and maps.

#### Integer Literals
Integer literals are sequences of digits or `0x` followed by hexadecimal digits. The value can be prefixed with `-` or `+` to indicate sign and is parsed as a 32-bit signed integer. If the value does not fit into a 32-bit signed integer, or the literal is suffixed with `L` or `l`, then the value is parsed as a 64-bit signed integer. Octal and binary integer literals as well as underscores within integer literals are not supported.

#### Double Literals
Double literals are sequences of digits, hexadecimal digits, `.`, exponents, and binary exponents as specified in float and double literals of C, C++, and Java. The value can be prefixed with `-` or `+` to indicate sign. Differences from the languages specifications include 1) literals containing a `.` must have digits (or hexadecimal digits) immediately preceding the `.` and digits (or hexadecimal digits) or an exponent (or binary exponent) immediately following the `.` (to reduce syntax ambiguity) and 2) literals can optionally end in either `d` or `D` in lieu of `f` or `F`, which affects the precision of the resulting value. Additionally, the literals `-Infinity` and `+Infinity` are supported. Long double literals as well as underscores within double literals are not supported.

#### String Literals
String literals are sequences of characters wrapped in either single quotes or double quotes. When wrapped in single quotes no escape sequences are allowed and the literal consists of the exact characters given. The following escape sequences are substituted for string literals wrapped in double quotes:
- `\\` - Backslash (`\`)
- `\"` - Double quote (`"`)
- `\'` - Single quote (`'`)
- `\b` - Backspace
- `\t` - Tab
- `\n` - Newline
- `\f` - Form feed
- `\r` - Carriage return
- `\0` - Null (Octal escape sequences not supported)
- `\xh...` - Unicode character with hex code point `h...`
- `\uhhhh` - Unicode character with hex code point `hhhh`
- `\Uhhhhhhhh` - Unicode character with hex code point `hhhhhhhh`

#### Regular Expression Literals
Regular expression literals use the form `~/[Pattern]/`, where `[Pattern]` is a valid `java.util.regex.Pattern`. Unicode character classes are enabled by default, but may be disabled by beginning the pattern with `(?-U)`. Literal forward slashes can be escaped using a preceding backslash (`~/."\/'\./` matches `a"/'.`).

#### Supported Operators
| Precedence | Operators | Associativity |
| ---------- | --------- | ------------- |
| 0 | <code>\{</code>a\*<code>\}</code> \(Set / Map Literal\), <br><code>\[</code>a\*<code>\]</code> \(List / Map Literal\), <br><code>\[:\]</code> \(Empty Map\), <br>a<code>\[</code>b<code>\]</code> \(Lookup\), <br>a<code>?\[?</code>b<code>\]</code> \(Safe Lookup\), <br>a<code>\(</code>b\*<code>\)</code> \(Call Method\), <br><code>\(</code>a<code>\)</code> \(Parentheses\), <br><code>~@</code>a \(Get Class\), <br>a<code>\.</code>b \(Navigate\), <br>a<code>?\.?</code>b \(Safe Navigate\) | Left&nbsp;to&nbsp;right |
| 2 | <code>\+</code>a \(Unary Plus\), <br><code>\-</code>a \(Unary Minus\), <br><code>~</code>a \(Bitwise Negate\), <br><code>\!</code>a \(Logical Negate\) | Right&nbsp;to&nbsp;left |
| 3 | a<code>\.\.</code>b \(Range\) | Left&nbsp;to&nbsp;right |
| 4 | a<code>\*</code>b \(Multiply\), <br>a<code>/</code>b \(Divide\), <br>a<code>%</code>b \(Modulus\) | Left&nbsp;to&nbsp;right |
| 5 | a<code>\+</code>b \(Add\), <br>a<code>\-</code>b \(Subtract\) | Left&nbsp;to&nbsp;right |
| 6 | a<code>&lt;&lt;</code>b \(Bitwise Shift Left\), <br>a<code>&gt;&gt;</code>b \(Bitwise Shift Right Sign Extend\), <br>a<code>&gt;&gt;&gt;</code>b \(Bitwise Shift Right Zero Extend\) | Left&nbsp;to&nbsp;right |
| 7 | a<code>&lt;=</code>b \(Less Than or Equal\), <br>a<code>&gt;=</code>b \(Greater Than or Equal\), <br>a<code>&lt;</code>b \(Less Than\), <br>a<code>&gt;</code>b \(Greater Than\) | Left&nbsp;to&nbsp;right |
| 8 | a<code>==</code>b \(Equal\), <br>a<code>\!=</code>b \(Not Equal\) | Left&nbsp;to&nbsp;right |
| 9 | a<code>&amp;</code>b \(Bitwise And\) | Left&nbsp;to&nbsp;right |
| 10 | a<code>^</code>b \(Bitwise Xor\) | Left&nbsp;to&nbsp;right |
| 11 | a<code>&#124;</code>b \(Bitwise Or\) | Left&nbsp;to&nbsp;right |
| 12 | a<code>&amp;&amp;</code>b \(Logical And\) | Left&nbsp;to&nbsp;right |
| 13 | a<code>&#124;&#124;</code>b \(Logical Or\) | Left&nbsp;to&nbsp;right |
| 14 | a<code>?:</code>b \(Null Coalesce\), <br>a<code>??</code>b \(Null Coalesce \- Alternate\), <br>a<code>?</code>b \(Ternary\), <br>a<code>:</code>b \(Pair\) | Right&nbsp;to&nbsp;left |
| 15 | a<code>=</code>b \(Bind Local Name\) | Right&nbsp;to&nbsp;left |
| 16 | a<code>,</code>b\* \(Item Separator\) | Left&nbsp;to&nbsp;right |
| 17 | <code>☠</code>a \(Die\), <br><code>~:&lt;</code>a \(Die \- Alternate\), <br><code>\#^</code>a \(Return\) | Left&nbsp;to&nbsp;right |
| 18 | a<code>;</code>b \(Statement Separator\) | Left&nbsp;to&nbsp;right |
| 19 | a<code>\#&gt;</code>b \(Streaming Remap\), <br>a<code>\#\.</code>b \(Streaming Remap \- Alternate\), <br>a<code>\#&#124;</code>b \(Streaming Flatten Remap\), <br>a<code>\#?</code>b \(Streaming Filter\), <br>a<code>\#&lt;</code>b \(Streaming Reduction\) | Left&nbsp;to&nbsp;right |

#### Named Expressions
Named expressions are tags with the form `{{ name -> expression }}` or `{{ name(param1, param2) -> expression }}`. (Unlike normal expressions, named expressions qualify for consideration as stand-alone tags.) The expression is bound to the specified name and can be used in later expressions (in both dynamic content tags and section tags).

Referencing a named expression using a function-like syntax evaluates the expression. The first argument is always pushed onto the context stack (if no first argument is given, the current context is repushed onto the context stack). For this reason, the first parameter in a named expression can be unnamed or specified as a literal `.`. It is not an error to mismatch the number of arguments with the number of parameters of the named expression. Unspecified parameters receive the value `null` upon invocation.

Named expressions are scoped to the context in which they are declared and can be overridden at lower level scopes. They always take precedence over equivalently named methods on the current context object. If a method is preferred over a named expression, it can be prefixed (using `./` or `../`), since named expressions can not be invoked using prefixes.

Root-level named expressions in each partial template are made available to the calling template when a partial is included (`{{> f }}` includes all root-level expressions from the partial "f" at the current scope). For example,
```horseshoe
{{< a }}
  {{ lower->toLowerCase() }}
{{/}}
{{ lower->toString() }}
{{ upper->toUpperCase() }}
{{# "Original String" }}
  {{> a }}
  {{ upper() + "-" + lower() }}
{{/}}
```
results in "  ORIGINAL STRING-original string", because the `lower` named expression is overridden when the partial `a` is included on line 7. This allows partials to contain either content to render or named expressions as a payload.

However, named expressions are not exposed to included partial templates. This is done so that all templates including partials are self-contained. For example,
```horseshoe
{{ func() -> "Hello!" }}
{{< a }}
  {{ func() }}
{{/ a }}
{{> a }}
```
results in a whitespace-only string, since `func()` is not defined within the partial `a`.

#### Streaming

Streaming operations can be used in expressions to transform, filter, or reduce data within a single expression. They can be used on iterables, arrays, or individual objects. Individual objects with `null` values are treated as absent.

Streaming transformations (`{{ a #> i -> transform(i) }}`) allow data to be remapped into a new form. The original item is replaced with the transformed item. Transformations can be used to consolidate section tags or to chain with filters and reductions to derive new data. The new list is the result of the operator. Flattening transformations (`{{ [[1, 2], [3, 4], null] #| i -> i `) allow lists to be combined.

Streaming filters (`{{# names #? name -> /* Find names with initials. */ ~/\b.[.]/.matcher(name).find() }}`) are used to filter out unneeded items. The new list is the result of the operator.

Streaming reductions (`{{ sum = 0; values #< value -> sum = sum + value }}`) allow a stream to be reduced to a single value. The result of the last iteration is the result of the operator.

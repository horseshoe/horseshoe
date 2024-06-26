# Horseshoe

Horseshoe for Java is a templating system used to generate source code and other dynamic content from a template and structured data.
It uses a Mustache-like syntax and extended expressions for dynamic data manipulation.

![Build](https://github.com/horseshoe/horseshoe/workflows/Build/badge.svg)
[![Codacy](https://app.codacy.com/project/badge/Grade/56988d0f17184c56bd1877a0c02002e1)](https://www.codacy.com/gh/horseshoe/horseshoe?utm_source=github.com&utm_medium=referral&utm_content=horseshoe/horseshoe&utm_campaign=Badge_Grade)
[![Coverage](https://app.codacy.com/project/badge/Coverage/56988d0f17184c56bd1877a0c02002e1)](https://www.codacy.com/gh/horseshoe/horseshoe?utm_source=github.com&utm_medium=referral&utm_content=horseshoe/horseshoe&utm_campaign=Badge_Coverage)
[![Coverity](https://scan.coverity.com/projects/21451/badge.svg)](https://scan.coverity.com/projects/horseshoe)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=horseshoe_horseshoe&metric=security_rating)](https://sonarcloud.io/dashboard?id=horseshoe_horseshoe)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=horseshoe_horseshoe&metric=bugs)](https://sonarcloud.io/dashboard?id=horseshoe_horseshoe)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=horseshoe_horseshoe&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=horseshoe_horseshoe)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=horseshoe_horseshoe&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=horseshoe_horseshoe)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=horseshoe_horseshoe&metric=alert_status)](https://sonarcloud.io/dashboard?id=horseshoe_horseshoe)
[![license](https://img.shields.io/github/license/horseshoe/horseshoe?label=License&logo=github)](https://github.com/horseshoe/horseshoe/blob/main/LICENSE)

## Goals

- Provide a Mustache-like template engine for generating source code.
- Support both static data and dynamically-generated data.
- Support common operator symbols to be used to support basic expressions and manipulations.
- Support iteration over iterable objects, including additional properties such as first/last/index.
- Allow iterations to be easily repeatable within a template.

## FAQ

### What is Horseshoe?

Horseshoe is a templating system used to generate content from a template and structured data.
It was primarily designed around the use case of quickly generating source code for languages like C++ and Java.
It is written in Java with a heavy emphasis on minimizing dependencies.
It uses a Mustache-like syntax and extended expressions that support method calls and operators to dynamically manipulate data.

### How is Horseshoe similar to Mustache?

Horseshoe uses the same tags as Mustache.
These tags have the same meaning as they do in Mustache.
Due to the extended expression syntax it is difficult to gauge whether or not it is Mustache-compliant.
However, when Mustache-compliant settings are used, Horseshoe passes all required tests in the Mustache specification v1.1.3.

### How is Horseshoe different from Mustache?

Horseshoe does not have the same design goals as Mustache, resulting in many different decisions when creating the specification.
For example, Horseshoe was designed for generation of source code, not HTML.
For this reason, content is not HTML-escaped by default.
(Although, settings can be created with `horseshoe.Settings.newMustacheSettings()` to support this feature.)
Other differences include a complex expression syntax ("interpolation" in Mustache-speak) and support for method calls in expressions.

### Does Horseshoe support Mustache lambdas?

Horseshoe does not support Mustache lambdas.
It foregoes lambdas in favor of an expression syntax that supports method calls and [anonymous partials](#section-partials).
[Expressions](#expressions) can be named and reused to support similar functionality to Mustache lambdas.
[Anonymous partials](#section-partials) are used to support rendering nested sections by applying a partial template.

### What literal types are supported in Horseshoe expressions?

The following table summarizes the literals supported by Horseshoe.
A more detailed explanation of each can be found in the [Expressions](#expressions) section.

| Horseshoe Type | Horseshoe Literal |
|-----------|-------------------|
| `int` | `123`, `1_203_937` |
| `long` | `123L` / `0x1_0000_0000` |
| `double` | `3.14` / `2'600.452'864f` |
| `boolean` | `true` / `false` |
| `Object` | `null` |
| `String` | `"a \"string\""` / `'a ''string'''` |
| `Class` | `~@name` |
| `Regular Expression` | `~/pattern/` |
| `List` | `[1,2,3,4]` |
| `Set` | `{1,2,3,4}` |
| `Map` | `[1:2,3:4]` / `[:]` |

### Which operators are supported by Horseshoe expressions?

Horseshoe supports most operators used in common languages with the exception of the assignment operator (`=`).
The assignment operator (`=`) can be used to bind a name to a value locally.
(Note that a bound name is scoped to a single expression and is not the same as an assignment operator.)
For more information on all the operators, see the [Supported Operators](#supported-operators) section.

### What file extension should be used for Horseshoe template files?

Horseshoe supports any file extension for template files.
However, convention is to use a capital "U", which resembles a horseshoe.

### How is whitespace within a template handled by Horseshoe?

Horseshoe uses the same whitespace paradigm as Mustache.
All tags except for content tags (and unescaped content tags) qualify for consideration as stand-alone tags.
In this case, the entire line is excluded from output and only the tag is processed by Horseshoe.
More details can be found in the [Tags](#tags) section.

## Code Example

The following example demonstrates how to use the Horseshoe library from within Java code.
[Template examples](#template-examples) are located in a separate section.

First, a template is loaded using the template loader class.
Templates can be loaded from a string, a file, or a reader.

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

Examples of Horseshoe templates can be found in the `samples/data` directory.
The `samples/data/results` directory contains the results of running these templates through the Horseshoe engine with default settings and an empty data map.
(Line endings may differ, as they vary by operating system when using default settings.)

The Horseshoe runner can be used to render templates from the command line.
The runner can be invoked using Java's `-jar` argument: `java -jar horseshoe.jar`.
It provides a subset of the rendering options available in the Horseshoe library.
A list of all available options for the runner can be listed using the `--help` argument.

## Description

Horseshoe is a Mustache-like templating system focused on fast source code generation.
The templates consist of mixed tags and content that are parsed and rendered using structured data.
An advanced expression syntax allows data to be dynamically generated and manipulated at render-time.
Rendered data can be output a number of different ways.
One way is to send output to an update-only file stream which will only modify a file if the content is updated.
This can improve performance when using timestamp-based build systems by eliminating unnecessary rebuilds.

### Tags

Horseshoe uses tags to specify dynamic parts of a template.
The tags typically start with `{{` and end with `}}`.
(The delimiters can be changed using the set delimiter tag.)
Those familiar with Mustache will recognize the majority of the tags used by Horseshoe.

All tags except for content tags may qualify as stand-alone tags.
A stand-alone tag is a tag that contains only whitespace before the opening braces on the beginning line and only whitespace after the closing braces on the ending line.
This holds true for tags that span multiple lines as well as when custom delimiters are used.

#### Comments

Comment tags (`{{! ignore this }}`) are structured the same as Mustache comments.
A comment tag begins with an exclamation point (`!`).

#### Content

Content tags (`{{ content }}`) are similar to Mustache interpolation tags.
The current context is used to look up the value specified inside the tag.
The value is then rendered in place of the tag.

There are a couple major differences from Mustache interpolation tags:

- The printed values are not HTML-escaped by default, since Horseshoe is designed for generating source code rather than HTML.
  Horseshoe can be configured to escape HTML if desired.

- The value specified inside the content tag represents a [Horseshoe expression](#expressions).
  Note that values such as `some-content` will be interpreted as the value `some` minus the value `content` rather than the value of `some-content`.
  This setting can be changed to match the Mustache interpolation tag if desired.
  Or the value can be quoted by wrapping the value in backticks (`` `some-content` ``).

Content tags can reference an explicit level within the context stack prefixing an identifier with `./`, `.\`, `../`, `..\`, `/`, or `\`.
For example, `{{ ..\content }}` will render the value of `content` from one level up in the context stack.
Leaving off the prefix informs Horseshoe to use the render settings when performing a lookup for an identifier.
The default settings will search for an identifier in the current level of the context stack, followed by the root level of the context stack.
See [Sections](#sections) for more details.

#### Unescaped Content

Unescaped content (`{{{ content }}}`, `{{& content }}`) is the same as normal content in Horseshoe, since content is not escaped by default.
It only differs from normal content if content escaping is enabled.
The tag can either start with an additional open curly brace (`{`) and end with an additional closing curly brace (`}`) or simply start with an ampersand (`&`).

#### Partials

Partial tags (`{{> partial }}`) are similar to partial tags in Mustache.
They function similarly to a `#include` directive in C++ but provide appropriate scoping.
The partial template is loaded either from the specified filename or the corresponding template from the template loader.
If the lookup name is not known until render-time, its value can be computed with an [expression](#expressions) surrounded by parentheses (`{{> (expression) }}`).
Once loaded, the partial is expanded into the current template.

If a partial tag is a stand-alone tag, the indentation of the partial tag will be prepended to every line of the partial template.
**Using a double arrow partial tag (`{{>> partial }}`) will avoid applying the indentation to every line but will ignore the trailing whitespace and newline after the partial.**

#### Set Delimiter (`{{=<% %>=}}`)

The set delimiter tag is structured the same as it is for Mustache.
It is used to change the delimiters from `{{` and `}}` to other sequences in templates that contain many double braces that are part of the literal text.
The new sequences are applicable for all tags.
Here is a brief example,

```horseshoe
{{! Spaces are allowed (but not required) after the equals sign. }}
{{= << >> =}}
{{ Literal template text. }}
<<! This is a comment and the line below is an unescaped content tag. >>
<<{ content }>>
More {{ literal }} template text.
<<! Return to using `{{` and `}}`. >>
<<={{ }}=>>
```

#### Sections

Section tags (`{{# map }}`) in Horseshoe are similar to Mustache section tags.
A section tag can either function as a conditional section or as a foreach section.
The contents of a conditional section are only rendered if the value is [truthy](#truthiness).
The contents of a foreach section are iterated once for every contained value.

Objects that result in foreach sections include arrays, iterables, and streams.
All other objects result in conditional sections.

All sections except for Booleans, [annotations](#annotations), and [section partials](#section-partials) push a value onto the context stack.
Foreach sections push each child item onto the stack as it iterates over the collection.
For example,

```horseshoe
{{# // Iterating through the list of maps below will result in each item being pushed onto the context stack.
	[['name': 'Esau', 'hairy': true, 'birthOrder': 'first'], ['name': 'Jacob', 'hairy': false, 'birthOrder': 'last'], ['name': 'Rebekah']] }}
	{{# hairy /* Booleans will NOT be pushed onto the context stack. */ }}
{{ name }} was hairy.
	{{^}}
{{ name }} was not hairy.
	{{/}}
	{{# birthOrder /* Non-Booleans WILL be pushed onto the context stack. */ }}
{{ ..\name }} was born {{.}}.
	{{^}}
Birth order of {{ name }} is not given.
	{{/}}
{{/}}
```

##### Internal Identifiers

Internal identifiers are used to get state information about the current section.
Iternal identifiers always begin with a period (`.`) to distinguish them from other identifiers.
A common example is getting the current iteration index of a foreach section, which can be done using the `.index` internal identifier.
A list of internal identifiers is given below.

| Identifier | Description |
| ---------- | ----------- |
| `.hasNext` | Returns `true` only if the foreach section has at least one more item to iterate. |
| `.indentation` | Gets the indentation of the scoped partial as a string. |
| `.index` | Gets the index of the foreach section. |
| `.isFirst` | Returns `true` only if the foreach section is iterating the first item. This is equivalent to `(.index == 0)`. |

Internal identifiers can be paired with a prefix to look up data in a higher-scoped section.
For example, `..\.index` can be used to look up the index of the section one level above the current section.

#### Repeated Sections

Repeated section tags (`{{#}}`) are used to duplicate the previous section at the same scope.
Nested scopes within a repeated section can also be repeated, allowing a hierarchical repeating of sections.
For example,

```horseshoe
<h1>Contents</h1>
{{# ['Feature 1', 'Feature 2', 'Feature 3'] }}
	<ul><li><a href="#{{ replace(' ', '') }}">{{.}}</a><ul>
	{{# ['x86', 'x86-64', 'ArmHf', 'Aarch64'] }}
		<li><a href="#{{ ..\replace(' ', '') }}_{{ replace(' ', '') }}">{{.}}</a></li>
	{{/}}
	</ul></li></ul>
{{/}}
{{#}}
<h1 id="{{ replace(' ', '') }}">{{.}}</h1>
	{{#}}
	<h2 id="{{ ..\replace(' ', '') }}_{{ replace(' ', '') }}">{{.}}</h2>
	{{/}}
{{/}}
```

#### Annotations

Annotations (`{{# @Annotation("Param1": false, "Param2") }}`) are identifiers that begin with an at sign (`@`) and are used to redirect the output of a section.
If the annotation does not exist, then the resulting value is `null`.
Otherwise, the arguments are parsed as a [Horseshoe expression](#expressions) and passed to the annotation handler (`null` if the parentheses and argument list are omitted), which returns a new writer.
**A `null` annotation or one that returns a `null` writer will cause the corresponding [inverted section](#inverted-sections) to be rendered, if it exists.**
Annotations do not affect the context stack, and output redirection only occurs for the scope of a section.

Built-in annotations include the following:

| Annotation | Description |
| ---------- | ----------- |
| `@Null` | Discards rendered output. |
| `@StdErr` | Sends rendered output to stderr using the default system character encoding. |
| `@StdOut` | Sends rendered output to stdout using the default system character encoding. |
| `@File('name': [filename], 'encoding': 'UTF-8', 'overwrite': false, 'append': false)` | Sends rendered output to the file with the specified name, using the specified encoding (defaults to 'UTF-8'). **If overwrite is false (or omitted) then the file is only modified if the rendered output is different from the current contents of the file.** If append is specified as true then the rendered output is appended to the file. |

#### Inverted Sections

Inverted section tags (`{{^ exists }}`) are used to negate a conditional or foreach section.
Inverted sections are only rendered when a section tag with the given expression would not be rendered.
Null objects, numeric zero values, `false`, as well as empty lists or arrays (see [Truthiness](#truthiness)) are several examples of values that will cause an inverted section to be rendered.
No value will be pushed onto the context stack in these situations.

An inverted section tag may start with a `#` (`{{^# condition }}`) to indicate an alternate condition or foreach section that will be evaluated when all conditionals associated with previous sections evaluate to `false` ("if-then-else" chaining).
An inverted section tag can be left empty (`{{^}}`) to indicate that the inverted section will be rendered when all conditionals associated with previous sections evaluate to `false`.
For example,

```horseshoe
{{# a }}
  a evaluates to true
{{^# b }}
  b evaluates to true
{{^}}
  a and b evaluate to false
{{/ b }}
```

An inverted section tag may also start with a `^` (`{{^^ condition }}`) to match a preceding condition from a `#` or `^#` tagged section.
This section is handled in the same way as a `{{^}}` section with the requirement that the `condition` matches the preceding section's expression.
This section's expression is not evaluated nor does `^^` change a section's scope, only the content is compared.
It is used to indicate to the reader the previous section's expression.
(An exception will be thrown if it does not match.)
For example,

```horseshoe
{{# a }}
  a evaluates to true
{{^^ a }}
  a evaluates to false
{{/ a }}
```

#### Inline Partials

Inline partial tags (`{{< partial }}`) define a partial template inline in the current template.
In this way, partial templates can be nested rather than loaded from another source, like a file.
Inline partials inherit the scope of the template in which they are declared, so named expressions and other inline partials can be used within the inline partial.

Inline partials can have named parameters ([template bindings](#template-bindings)).
The first parameter can be specified as a literal `.` since the first parameter will always be pushed onto the context stack when the partial is included.

**Closing tags for inline partials are always considered stand-alone tags for trailing whitespace purposes even if other content precedes them.**
This allows inline partials to end without a trailing newline and not cause an output line in the current template.

Inline partials can be expanded using a [partial tag](#partials).
An inline partial from an external template can be expanded by specifying the name of the external template followed by a `/` before the name of the inline partial.
Passing arguments to an inline partial is done using an [expression](#expressions) surrounded by parentheses.
Computing the name of an inline partial can also be done using a parenthesized [expression](#expressions).
For example,

```horseshoe
{{> MyPartial(1 + 1, 'second arg') }}
{{> OtherTemplateFile.U/MyPartial(1 + 1, 'second arg') }}
{{> ('My' + 'Partial')(1 + 1, 'second arg') }}
```

#### Section Partials

Section partial tags (`{{#> partial }}`) apply a partial template to a nested section.
This provides some functionality similar to a Mustache lambda, except that no code is used to render the section.
It is all within the confines of the Horseshoe template.

In order to support rendering the nested section, the referenced partial template uses an anonymous partial, which is a partial tag without a name.
For example,

```horseshoe
{{< dash }}
---{{>}}---{{/}}
{{#> dash }}a{{/}}, {{#> dash }}b{{/}}, {{#> dash }}c{{/}}
```

results in

```text
---a---, ---b---, ---c---
```

**Closing tags for section partials are always considered stand-alone tags for trailing whitespace purposes even if other content precedes them.**
This allows section partials to end without a trailing newline and not cause an output line in the current template.

### Expressions

Horseshoe expressions look a lot like expressions in modern programming languages.
Expressions can contain named values, literals, method calls, and operator symbols.
Expressions can also contain comments using C, C++, and Java style comments: `/*...*/` or `//...`.
A single expression can consist of multiple lines.

Multiple statements can be chained together inside an expression using a semicolon (`;`) as a separator.
A trailing semicolon at the end of the expression is not needed.
A comma (`,`) is used to separate method parameters and list or map entries.

#### Truthiness

Truthiness in Horseshoe is dependent on the type of the object being coerced to a Boolean value.
For Booleans, no coersion is needed.
For numeric or character primitives, `true` is coerced for non-zero values.
For a `java.util.Optional`, `true` is coerced for values that are present.
For collections, arrays, maps, and strings, `true` is coerced for values that have a size or length greater than zero.
All other objects are coerced to `true` for any non-`null` value.
All other values are coerced to `false`.

#### List, Map, and Set Literals

Horseshoe expressions use `[]` for list / map literals or `{}` for set / map literals.
Any list or set that contains a colon (`:`) separator is treated as a map.
Entries in a map literal without a colon are treated as if the given item is used as both the key and the value.

Commas can be used anywhere within an expression.
If a comma is used in a context where a comma is unexpected (e.g., `{{ 4, 5 }}`) the result is interpreted as if it were wrapped in `[]` to form a list or map.
These are considered auto-converted lists and maps.

Lists, maps, and sets can be added and subtracted to form new lists, maps, and sets (`[1, 2] + {2, 3, 4} - [3]`).
They can be sliced by range (`list[1:2]`) or by another list, map, or set (`map[{1, 3, 'Apple'}]`) using the [lookup operator](#supported-operators).

#### Integer Literals

Integer literals are sequences of digits or `0x` followed by hexadecimal digits.
The value can be prefixed with `-` or `+` to indicate sign and is parsed as a 32-bit signed integer.
If the value does not fit into a 32-bit signed integer, or the literal is suffixed with `L` or `l`, then the value is parsed as a 64-bit signed integer.
Underscores or single quotes may be used as digit separators after the first digit.
Octal and binary integer literals are not supported.

#### Double Literals

Double literals are sequences of digits, hexadecimal digits, `.`, exponents, and binary exponents as specified in float and double literals of C, C++, and Java.
The value can be prefixed with `-` or `+` to indicate sign.
Differences from the languages specifications include the following:

- Literals containing a `.` must have digits (or hexadecimal digits) immediately preceding the `.` and digits (or hexadecimal digits) or an exponent (or binary exponent) immediately following the `.` (to reduce syntax ambiguity).
- Literals can optionally end in `d`, `D`, `f`, or `F`, which affects the precision of the resulting value.

Additionally, the literals `-Infinity` and `+Infinity` are supported.
Underscores or single quotes may be used as digit separators after the first digit.
Long double literals are not supported.

#### String Literals

String literals are sequences of characters wrapped in either single quotes or double quotes.
When wrapped in single quotes the only escape sequence is `''` (double single quote), which escapes a single quote.
All other characters in a single-quoted string literal are the exact characters given.
The following escape sequences can be used in double-quoted string literals:

| Escape | Replacement |
| ------ | ------------ |
| `\\` | Backslash (`\`) |
| `\"` | Double quote (`"`) |
| `\'` | Single quote (`'`) |
| `\b` | Backspace |
| `\t` | Tab |
| `\n` | Newline |
| `\f` | Form feed |
| `\r` | Carriage return |
| `\$` | Dollar sign (`$`) |
| `\{` | Open curly brace (`{`) |
| `\}` | Close curly brace (`}`) |
| `\0` | Null (Octal escape sequences not supported) |
| `\xh...` | Unicode character with hex code point `h...` |
| `\uhhhh` | Unicode character with hex code point `hhhh` |
| `\Uhhhhhhhh` | Unicode character with hex code point `hhhhhhhh` |

##### String Interpolation

String interpolation is used to embed expressions within a double-quoted string literal using a placeholder.
Each placeholder starts with a dollar sign (`$`) and is followed by either an identifier name or curly braces (`{`, `}`) surrounding a Horseshoe expression.
If a dollar sign (`$`) is not followed by an identifier name or curly braces (`{`, `}`), it is treated as a literal dollar sign (`$`).
The dollar sign (`$`) and curly brace (`{`, `}`) characters can be escaped by prepending the character with a backslash (`\`).

For example, `"String interpolation can be done using $[identifier] ($identifier) or \${ [expression] } (${ 5 + 2 }), but is ignored for the literal \"$4.50\"."` results in `String interpolation can be done using $[identifier] (null) or ${ [expression] } (7), but is ignored for the literal "$4.50".`.

#### Class Literals

Class literals (`~@[ClassName]`) can be used to reference a class.
For example, `~@Integer.MAX_VALUE` can be used to get the max value of a 32-bit signed int, and `~@BigInteger.new('12345678909876543211234567890')` can be used to create a new `BigInteger`.
Class literals are closely related to the "Get Class" operator.
The only difference being that the operator takes a string as its operand.

By default, only a limited number of classes can be loaded.
Additional classes can be added to the Horseshoe settings, so that they can be loaded as well.
Another setting can be modified to only allow loading a class by its fully-qualified name, which disables the use of class literals.

#### Regular Expression Literals

Regular expression literals use the form `~/[Pattern]/`, where `[Pattern]` is a valid `java.util.regex.Pattern`.
Unicode character classes are enabled by default, but may be disabled by beginning the pattern with `(?-U)`.
Literal forward slashes can be escaped using a preceding backslash (`~/."\/'\./` matches `a"/'.`).

#### Supported Operators

| Precedence | Operators | Associativity |
| ---------- | --------- | ------------- |
| 0 | `{` a\* `}` \(Set / Map Literal\), <br>`[` a\* `]` \(List / Map Literal\), <br>`[:]` \(Empty Map\), <br>a `[` b `]` \(Lookup\), <br>a `?[` b `]` \(Safe Lookup\), <br>a `[?` b `]` \(Nullable Lookup\), <br>a `(` b\* `)` \(Call Method\), <br>`(` a `)` \(Parentheses\), <br>`~@` a \(Get Class\), <br>a `.` b \(Navigate\), <br>a `?.` b \(Safe Navigate\), <br>a `.?` b \(Nullable Navigate\) | Left&nbsp;to&nbsp;right |
| 1 | a `**` b \(Exponentiate\) | Left&nbsp;to&nbsp;right |
| 2 | `+` a \(Unary Plus\), <br>`-` a \(Unary Minus\), <br>`~` a \(Bitwise Negate\), <br>`!` a \(Logical Negate\) | Right&nbsp;to&nbsp;left |
| 3 | a `..` b \(Integer Range\), <br>a `..<` b \(Exclusive Integer Range\) | Left&nbsp;to&nbsp;right |
| 4 | a `*` b \(Multiply\), <br>a `/` b \(Divide\), <br>a `%` b \(Modulus\) | Left&nbsp;to&nbsp;right |
| 5 | a `+` b \(Add\), <br>a `-` b \(Subtract\) | Left&nbsp;to&nbsp;right |
| 6 | a `<<` b \(Bitwise Shift Left\), <br>a `>>` b \(Bitwise Shift Right Sign Extend\), <br>a `>>>` b \(Bitwise Shift Right Zero Extend\) | Left&nbsp;to&nbsp;right |
| 7 | a `<=>` b \(Three\-way Comparison\) | Left&nbsp;to&nbsp;right |
| 8 | a `<=` b \(Less Than or Equal\), <br>a `>=` b \(Greater Than or Equal\), <br>a `<` b \(Less Than\), <br>a `>` b \(Greater Than\), <br>a `in` b \(Is In\) | Left&nbsp;to&nbsp;right |
| 9 | a `==` b \(Equal\), <br>a `!=` b \(Not Equal\), <br>a `=~` b \(Find Pattern\), <br>a `==~` b \(Match Pattern\) | Left&nbsp;to&nbsp;right |
| 10 | a `&` b \(Bitwise And\) | Left&nbsp;to&nbsp;right |
| 11 | a `^` b \(Bitwise Xor\) | Left&nbsp;to&nbsp;right |
| 12 | a `\|` b \(Bitwise Or\) | Left&nbsp;to&nbsp;right |
| 13 | a `&&` b \(Logical And\) | Left&nbsp;to&nbsp;right |
| 14 | a `\|\|` b \(Logical Or\) | Left&nbsp;to&nbsp;right |
| 15 | a `?:` b \(Elvis\), <br>a `??` b \(Null Coalescing\), <br>a `!:` b \(Inverted Elvis\), <br>a `!?` b \(Non\-null Coalescing\), <br>a `?` b \(Ternary\), <br>a `:` b \(Pair / Range\), <br>a `:<` \(Backward Range\) | Right&nbsp;to&nbsp;left |
| 16 | `☠` a \(Die\), <br>`~:<` a \(Die \- Alternate\), <br>`#^` a \(Return\) | Left&nbsp;to&nbsp;right |
| 17 | a `#>` b \(Streaming Remap\), <br>a `#.` b \(Streaming Remap \- Alternate\), <br>a `#\|` b \(Streaming Flatten Remap\), <br>a `#?` b \(Streaming Filter\), <br>a `#<` b \(Streaming Reduction\) | Left&nbsp;to&nbsp;right |
| 18 | a `=` b \(Bind Local Name\) | Right&nbsp;to&nbsp;left |
| 19 | a `,` b\* \(Item Separator\) | Left&nbsp;to&nbsp;right |
| 20 | a `;` b \(Statement Separator\) | Left&nbsp;to&nbsp;right |

Many operators support more operand types than traditional programming languages.
Addition and subtraction can be applied to lists, maps, and sets as well as numeric primitives.
Also, comparison operators can be used to compare enumerations with strings as well as comparing numeric primitives, strings, and any comparable objects.

##### Ternary Operator

The ternary operator and ternary-related operators (Elvis and Null-coalescing operators and their inverse operators) are used to emulate "if-then-else" logic.
The ternary operator is the only operator with three operands and is expressed in the following form: `[condition] ? [true-result] : [false-result]`.
It is a short-circuiting operation, so `[true-result]` is only evaluated when `[condition]` evaluates to `true` according to it's [truthiness](#truthiness).
Otherwise, `[false-result]` is evaluated.

The ternary-related operators perform the same "if-then-else" logic while only using two operands.
It is important to note that the condition (left hand side) operand of the operator is only evaluated once.
So, any side effects caused by the left operand will occur exactly once.
The following table shows the ternary equivalent of each ternary-related operator:

| Expression | Ternary Equivalent |
| ---------- | ------------------ |
| `a ?: b` | `a ? a : b` |
| `a ?? b` | `a != null ? a : b` |
| `a !: b` | `!a ? null : b` |
| `a !? b` | `a == null ? null : b` |

For example, the equivalent of `if (a) { 'a is true' }` is `a !: 'a is true'`.

##### Safe and Nullable Operators

Safe operators return `null` rather than throwing an exception if the left hand side of the operation is `null`.
A safe operator is equivalent to prefixing the operation with a `null` check that short circuits the operation by returning `null`.
Essentially, `lhs == null ? null : (lhs <op> rhs)`.

Nullable operators are similar to safe operators in that they return `null` rather than throw a `NullPointerException`.
However, nullable operators also return `null` when failing to resolve interpolated values or methods on the right side of the operator as well as when encountering a `null` value on the left side of an operator.
For example, `{{ a.?b() }}` would return `null` if either `a` is `null` or `a` does not contain a method `b()`.

#### Method Calls

Method calls can be invoked on an object using the navigate operator with parentheses and a list of parameters.
The method `new` can be used to create new objects of a given type.
For example, `~@Integer.new('5')` creates a new `Integer` with the value `5`.

Method calls use a dynamic dispatch mechanism based on the type of the target object and the number of parameters.
When multiple matching methods are found, each is tried until one succeeds without throwing an exception.
Methods can be disambiguated by specifying the parameter types using a special syntax.
The method name is followed by a colon and comma-separated list of simple or canonical type names and wrapped in backticks.

For example, ``~@Math.`min:int`(1, 5)`` can be used to specify the `min` method that takes 2 `int` parameters, and ``~@Math.`min:double,double`(1.0, 5.0)`` can be used to specify the `min` method that takes 2 `double` parameters.
The first example shows that not all parameters need to be specified.
The second example shows that multiple parameter types are separated by commas and no spacing is allowed.

#### Built-In Properties

Built-in properties are properties that can be accessed via the navigate operator (`.`) for built-in types.
For example, `{{# map.entries }}` will access the entries in a map and iterate over them in the section, rather than looking up the key-value pairs in the section.
A list of the built-in properties and the applicable types are given in the table below:

| Property | Horseshoe Type |
|-----------|-------------------|
| `entries` | `Map` |
| `key` | `Entry` |
| `length`, `size` | `List`, `Map`, `Set`, `String` |
| `value` | `Entry` |

#### Streaming

Streaming operations can be used in expressions to transform (`#>`, `#.`, `#|`), filter (`#?`), or reduce (`#<`) data within a single expression.
They are followed by an optional identifier and arrow (`value ->`, `->`, and \[empty\] are all allowed) and can be used to stream iterables, arrays, or individual objects.
Individual objects with `null` values are treated as absent.

Streaming transformations (<code>{{ a **#>** i -> transform(i) }}</code>) allow data to be remapped into a new form.
The original item is replaced with the transformed item.
Transformations can be used to consolidate section tags or to chain with filters and reductions to derive new data.
The new list is the result of the operator.
Flattening transformations (<code>{{ [1, null, 3] **#|** i -> i !? [i, i + 1] // 1: [1, 2], null: null, 3: [3, 4] -> [1, 2, 3, 4] }}</code>) allow lists to be combined.

Streaming filters (<code>{{# names **#?** name -> /\* Find names with initials. \*/ ~/\b.[.]/.matcher(name).find() }}</code>) are used to filter out unneeded items.
The new list is the result of the operator.

Streaming reductions (<code>{{ sum = 0; values **#&lt;** value -> sum = sum + value }}</code>) allow a stream to be reduced to a single value.
The result of the last iteration is the result of the operator.

### Named Expressions

Named expressions are tags with the form `{{ name() -> expression }}` or `{{ name(param1, param2) -> expression }}`.
(Unlike normal expressions, named expressions qualify for consideration as stand-alone tags.)
The expression is bound to the specified name and can be used in later expressions (in both dynamic content tags and section tags).
Referencing a named expression using a function-like syntax evaluates the expression.
For example,

```horseshoe
{{ lower() -> toLowerCase() }}
{{ upper() -> toUpperCase() }}
{{# "Original String" }}
  {{ upper() + "-" + lower() }}
{{/}}
```

results in

```text
  ORIGINAL STRING-original string
```

The first argument to a named expression is pushed onto the context stack.
(If no first argument is given, the context stack is not modified.)
For this reason, the first parameter in a named expression tag can be specified as a literal `.`.
It is not an error to mismatch the number of arguments in a named expression invocation with the number of parameters in a named expression tag.
Unspecified parameters receive the value `null` upon invocation.

Named expressions are scoped to the template in which they are declared.
If a named expression with the same name already exists in the template, then attempting to redeclare it will result in an error.
Named expressions always take precedence over equivalently named methods on the current context object.
If a method is preferred over a named expression, it can be prefixed (using `.\`, `..\`, or `\`), since named expressions can not be invoked using prefixes.

Root-level named expressions can be imported into a template using [partial tags](#partials).
Named expressions can be imported individually, using `{{> f | MyExpression() }}`, or collectively, using `{{> f | * }}`.
This allows templates to contain either templated content or named expressions (or both) as a payload.

Named expressions are inherited by all [inline partials](#inline-partials) inside a template.
For example,

```horseshoe
{{ func() -> "Hello!" }}
{{< b }}
  {{ func() }}
{{/ b }}
{{> b }}
```

results in

```text
  Hello!
```

This output is produced because `func()` is inherited by the partial `b`.
However, the named expression `func` would **not** be accessible from an external template included using a [partial tag](#partials).

### Template Bindings

Template bindings are tags with the form `{{ name := expression }}`.
(Unlike normal expressions, template bindings qualify for consideration as stand-alone tags.)
The expression is bound to the specified name and can be used in later expressions within the template or any inline partial templates.

Template bindings are scoped to the template in which they are declared and can only be accessed after they are declared.
They can be referenced inside inline partial templates.
Inline partials and templates will get their own copies of any template bindings, similar to local variables inside a function.
This is also true for recursively-invoked partials and templates.

## Docker Image

The Horseshoe docker image executes the runner using the given run arguments.
Files can be mounted into the container using the `--volume` option.
For example, `docker run -v ~/horseshoe_data:/data horseshoe/horseshoe /data/input.U -o /data/output.cxx` reads the file `~/horseshoe_data/input.U` and writes the results to the file `~/horseshoe_data/output.cxx`.

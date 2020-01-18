## Note: This is a pre-release version, use at your own risk

# Horseshoe
Horseshoe for Java

![Build](https://github.com/nicklauslittle/horseshoe-j/workflows/Build/badge.svg)
[![Codacy](https://api.codacy.com/project/badge/Grade/07cec89cb05f4ed4ba8759f6ad8bdc97)](https://www.codacy.com/manual/nicklaus.little/horseshoe-j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=nicklauslittle/horseshoe-j&amp;utm_campaign=Badge_Grade)
[![Coverity](https://scan.coverity.com/projects/20222/badge.svg)](https://scan.coverity.com/projects/nicklauslittle-horseshoe-j)

## Goals
*   Mustache-like templates for source code
*   Allow method calls and processing of returned values
*   Simple iteration and repeated iterations with special cases based on first/last/index

## FAQ
### What is Horseshoe?
Horseshoe is a templating system used to generate source code. It uses a Mustache-like syntax with an extended expression system supporting method calls to allow processing of both stored data and dynamically-generated data.

### How is Horseshoe similiar to Mustache?
Horseshoe uses the same tags as Mustache. These tags have the same meaning as they do in Mustache. Due to the extended expression syntax, it is difficult to gauge whether or not it is Mustache-compliant. However, when a Mustache-compliant context object is used, Horseshoe passes all required tests in the Mustache specification v1.1.2.

### How is Horseshoe different from Mustache?
Horseshoe does not have the same design goals as Mustache, resulting in many different decisions when creating the specification. For example, Horseshoe was designed for generation of source code, not HTML. For this reason, the default escape function does not escape HTML (unless the settings are created with `horseshoe.Settings.newMustacheSettings()`). Other differences include a more complex expression syntax ("interpolation" in Mustache-speak) and support for method calls in expressions.

### Does Horseshoe support Mustache lambdas?
Horseshoe does not support Mustache lambdas. It foregoes lambdas in favor of an expression syntax that supports method calls. Expressions can be named and reused to support similar functionality to Mustache lambdas.

### What literal types are supported in Horseshoe expressions?
| Java Type | Horseshoe Literal |
|-----------|------|
| `java.lang.String` | `"a string"` |
| T`[]` | `[1,2,3]` / `[1..3]` / `[]` |
| `java.util.Map` | `[1:2,3:4]` / `[:]` |
| `int` | `123` |
| `double` | `3.14` |
| `boolean` | `true` / `false` |
| `java.lang.Object` | `null` |

### Which operators are supported by Horseshoe expressions?
| Precedence | Operators | Associativity |
| ---------- | --------- | ------------- |
| 0 | <code>{</code>a?<code>}</code> \(Array / Map Literal \(Iterating\)\), <br><code>[</code>a?<code>]</code> \(Array / Map Literal\), <br><code>[:]</code> \(Empty Map\), <br>a<code>[</code>b<code>]</code> \(Lookup\), <br>a<code>?[</code>b<code>]</code> \(Safe Lookup\), <br>a<code>(</code>b?<code>)</code> \(Call Method\), <br><code>(</code>a<code>)</code> \(Parentheses\), <br>a<code>.</code>b \(Navigate\), <br>a<code>?.</code>b \(Safe Navigate\) | Left&nbsp;to&nbsp;right |
| 2 | <code>+</code>a \(Unary Plus\), <br><code>-</code>a \(Unary Minus\), <br><code>~</code>a \(Bitwise Negate\), <br><code>!</code>a \(Logical Negate\) | Right&nbsp;to&nbsp;left |
| 3 | a<code>..</code>b \(Range\) | Left&nbsp;to&nbsp;right |
| 4 | a<code>*</code>b \(Multiply\), <br>a<code>/</code>b \(Divide\), <br>a<code>%</code>b \(Modulus\) | Left&nbsp;to&nbsp;right |
| 5 | a<code>+</code>b \(Add\), <br>a<code>-</code>b \(Subtract\) | Left&nbsp;to&nbsp;right |
| 6 | a<code>&lt;&lt;</code>b \(Bitwise Shift Left\), <br>a<code>&gt;&gt;</code>b \(Bitwise Shift Right Sign Extend\), <br>a<code>&gt;&gt;&gt;</code>b \(Bitwise Shift Right Zero Extend\) | Left&nbsp;to&nbsp;right |
| 7 | a<code>&lt;=</code>b \(Less Than or Equal\), <br>a<code>&gt;=</code>b \(Greater Than or Equal\), <br>a<code>&lt;</code>b \(Less Than\), <br>a<code>&gt;</code>b \(Greater Than\) | Left&nbsp;to&nbsp;right |
| 8 | a<code>==</code>b \(Equal\), <br>a<code>!=</code>b \(Not Equal\) | Left&nbsp;to&nbsp;right |
| 9 | a<code>&amp;</code>b \(Bitwise And\) | Left&nbsp;to&nbsp;right |
| 10 | a<code>^</code>b \(Bitwise Xor\) | Left&nbsp;to&nbsp;right |
| 11 | a<code>&#124;</code>b \(Bitwise Or\) | Left&nbsp;to&nbsp;right |
| 12 | a<code>&amp;&amp;</code>b \(Logical And\) | Left&nbsp;to&nbsp;right |
| 13 | a<code>&#124;&#124;</code>b \(Logical Or\) | Left&nbsp;to&nbsp;right |
| 14 | a<code>?:</code>b \(Null Coalesce\), <br>a<code>??</code>b \(Null Coalesce \(Alternate\)\), <br>a<code>?</code>b \(Ternary\), <br>a<code>:</code>b \(Pair\) | Right&nbsp;to&nbsp;left |
| 16 | a<code>,</code>b? \(Array / Map Separator\), <br>a<code>;</code>b \(Statement Separator\) | Left&nbsp;to&nbsp;right |

### What extension should be used for Horseshoe template files?
Horseshoe supports any file extension for template files. However, convention is to use a capital "U", which resembles a horseshoe.

### How is whitespace within a template handled by Horseshoe?
Horseshoe uses the same whitespace paradigm as Mustache. All tags except for content (and unescaped content) and partial tags qualify for consideration as standalone tags, meaning only whitespace and that tag occur on a single line. In this case, the entire line is excluded from output and only processed by Horseshoe.

## Example
First, a template is loaded using the template loader class. Templates can be loaded from a string, a file, or a reader.
```java
final horseshoe.Template template = new horseshoe.TemplateLoader().load("Hello World", "{{{salutation}}}, {{ recipient }}!");
// final horseshoe.Template mustacheTemplate = horseshoe.TemplateLoader.newMustacheLoader().load("Hello World", "{{{salutation}}}, {{ recipient }}!");
```

Next, a data map is created that contains all the data used to render the template.
```java
final java.util.Map<String, Object> data = new java.util.HashMap<>();
data.put("salutation", "Hello");
data.put("recipient", "world");
```

Finally, the template is rendered to a writer using the settings and the data.
```java
final horseshoe.Settings settings = new horseshoe.Settings();
// final horseshoe.Settings mustacheSettings = horseshoe.Settings.newMustacheSettings();
final java.io.StringWriter writer = new java.io.StringWriter();

template.render(settings, data, writer);
System.out.println(writer.toString()); // Prints "Hello, world!"
```

## Description
Horseshoe is a Mustache-like templating system focused on fast source code generation. It is written in Java with an emphasis on minimizing dependencies.

### Tags
Horseshoe uses "tags" to specify dynamic parts of a template. The tags typically start with `{{` and end with `}}`. Those familiar with Mustache will recognize the majority of the tags used by Horseshoe.

#### Comment (`{{! ignore this}}`)

#### Content (`{{content}}`)
#### Unescaped Content (`{{{content}}}`, `{{& content}}`)
#### Partial (`{{> partial}}`)
#### Set Delimiter (`{{=<% %>=}}`)
#### Section (`{{# map}}`)
#### Inverted Section (`{{^ exists}}`)
#### Inline Partial (`{{< partial}}`)

### Expressions

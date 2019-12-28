## Note: This is a pre-release version, use at your own risk

# Horseshoe
Horseshoe for Java

## Goals
* Mustache-like templates for source code
* TODO

## FAQ
### What is Horseshoe?
Horseshoe is a templating system used to generate source code. It uses a Mustache-like syntax with an extended expression system supporting method calls to allow processing of both stored data and dynamically-generated data.

### How is Horseshoe similiar to Mustache?
Horseshoe uses the same tags as Mustache. These tags have the same meaning as they do in Mustache. Due to the extended expression syntax, it is difficult to gauge whether or not it is Mustache-compliant. However, when the appropriate context object is used, Horseshoe passes all required tests in the Mustache specification v1.1.2.

### How is Horseshoe different from Mustache?
Horseshoe does not have the same design goals as Mustache, resulting in many different decisions when creating the specification. For example, Horseshoe was designed for generation of source code, not HTML. For this reason, the default escape function does not escape HTML (unless the context is created with `horseshoe.Context.newMustacheContext()`). Other differences include a more complex expression syntax ("interpolation" in Mustache-speak), support for limited logic, support for method calls in expressions, and support for variable assignment inside expressions.

### Does Horseshoe support Mustache lambdas?
Horseshoe does not support Mustache lambdas. It supplements lambdas with a more sophisticated expression system.

### What operators does Horseshoe support?
Precedence | Operators | Associativity
---------- | --------- | -------------
0 | <code>{</code>a?<code>}</code> (Map / Set Literal), <br>a<code>(</code>b?<code>)</code> (Call Method), <br><code>(</code>a<code>)</code> (Parentheses), <br>a<code>.</code>b (Navigate) | Left-to-right
2 | <code>+</code>a (Unary Plus), <br><code>-</code>a (Unary Plus), <br><code>~</code>a (Bitwise Negate), <br><code>!</code>a (Logical Negate) | Right-to-left
4 | a<code>*</code>b (Multiply), <br>a<code>/</code>b (Divide), <br>a<code>%</code>b (Modulus) | Left-to-right
5 | a<code>+</code>b (Add), <br>a<code>-</code>b (Subtract) | Left-to-right
6 | a<code><<</code>b (Bitwise Shift Left), <br>a<code>>></code>b (Bitwise Shift Right Sign Extend), <br>a<code>>>></code>b (Bitwise Shift Right Zero Extend) | Left-to-right
7 | a<code><=</code>b (Less Than or Equal), <br>a<code>>=</code>b (Greater Than or Equal), <br>a<code><</code>b (Less Than), <br>a<code>></code>b (Greater Than) | Left-to-right
8 | a<code>==</code>b (Equal), <br>a<code>!=</code>b (Not Equal) | Left-to-right
9 | a<code>&</code>b (Bitwise And) | Left-to-right
10 | a<code>^</code>b (Bitwise Xor) | Left-to-right
11 | a<code>&#124;</code>b (Bitwise Or) | Left-to-right
12 | a<code>&&</code>b (Logical And) | Left-to-right
13 | a<code>&#124;&#124;</code>b (Logical Or) | Left-to-right
14 | a<code>:</code>b (Pair) | Right-to-left
16 | a<code>,</code>b (Statement Separator) | Left-to-right

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

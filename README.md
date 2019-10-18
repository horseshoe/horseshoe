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

## Example
First, a new context must be created. Contexts are used to provide properties for how templates are loaded and rendered. A different context can be used for loading and rendering.
```java
final horseshoe.Context context = new horseshoe.Context();
// final horseshoe.Context mustacheContext = horseshoe.Context.newMustacheContext();
```

Next, a template is loaded using the template class constructor. A template can be loaded from a string, a file, or a reader.
```java
final horseshoe.Template template = new horseshoe.Template("Hello World", "{{{salutation}}}, {{ recipient }}!", context);
```

Finally, the template is rendered to a writer using the context. The context must contain all data used to render the template.
```java
context.put("salutation", "Hello");
context.put("recipient", "world");

final java.io.StringWriter writer = new java.io.StringWriter();
template.render(context, writer);
System.out.println(writer.toString()); // Prints "Hello, world!"
```

# Lexical Assert

A structural, offset-aware code comparison utility for Java, powered by the Myers diff algorithm.

`LexicalAssert` solves the classic problem of unit testing code generators and complex multi-line strings: **How do you verify the structure and content of generated code without making your tests brittle to whitespace, indentation, or visual formatting?**

Standard string equality (`assertEquals`) fails if the indentation changes by a single space. Stripping all whitespace makes the test pass, but completely destroys the readability of the failure output when things actually go wrong.

`LexicalAssert` gives you the best of both worlds:
1. **Structural Comparison**: It tokenizes your code (ignoring formatting whitespace) to perform a robust semantic comparison. `a.b` equals `a  .  b`.
2. **Beautiful Diffs**: When a mismatch occurs, it uses Myers diffing to surgically splice the *actual* differing tokens into the *expected* string's character offsets. Your test failure output preserves all of your expected visual spacing, indentation, and line breaks, making it effortless to read in your IDE's diff viewer.

## Installation

Add the dependency to your `pom.xml` (usually in the `test` scope):

```xml
<dependency>
    <groupId>dev.akre.test</groupId>
    <artifactId>lexical-assert</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

## Usage

### Basic Assertion

```java
import dev.akre.test.LexicalAssert;
import dev.akre.test.Tokenizer;
import org.junit.jupiter.api.Test;

public class CodeGenTest {
    @Test
    public void testGeneratedCode() {
        // You can format your expected string however you want for readability
        String expected = """
            public class Foo {
                public void bar() {}
            }""";

        String generatedCode = "public class Foo {public void bar(){}}";

        // This passes! The tokens match, ignoring the whitespace differences.
        LexicalAssert.assertStructuralEquals(Tokenizer.JAVA_CODE, expected, generatedCode);
    }
}
```

### The "Smart Diff" in Action

When a test fails, `LexicalAssert` throws an `AssertionFailedError` (from `opentest4j`, ensuring seamless integration with IntelliJ, Eclipse, and VSCode diff viewers).

```java
String expected = """
    public class Foo {
        public void bar() {}
    }""";

String actual = "public class Bar {public void baz(){}}";

LexicalAssert.assertStructuralEquals(Tokenizer.JAVA_CODE, expected, actual);
```

**The resulting diff viewer will show:**
```diff
- public class Foo {
-     public void bar() {}
+ public class Bar {
+     public void baz() {}
  }
```
*Notice how the actual tokens (`Bar`, `baz`) are spliced into the expected formatting!*

## Built-in Tokenizers

The library comes with several pre-configured tokenizers out of the box:

*   **`Tokenizer.JAVA_CODE`**: Handles strings, characters, standard identifiers, numbers, and symbols.
*   **`Tokenizer.JSON`**: Tailored for JSON payloads. Treats `-` and `.` as distinct punctuation to maintain strict structural bounds.
*   **`Tokenizer.SQL`**: Tailored for ANSI SQL. Handles `'...'` strings (with `''` escapes), `""` quoted identifiers, and standard symbols. Unquoted identifiers are compared case-insensitive. 

### Custom Tokenizers

You can easily create your own tokenizer using regex. The tokenizer must capture semantic tokens (words, symbols, strings) and ignore the whitespace between them.

```java
Tokenizer customTokenizer = Tokenizer.ofRegex(
    "(?x) [a-zA-Z0-9]+ | [^a-zA-Z0-9\\s]"
);
```

## License
Apache License 2.0

package dev.akre.test;

import org.assertj.core.api.AbstractAssert;
import org.opentest4j.AssertionFailedError;

/**
 * AssertJ-style assertions for structural string comparison.
 */
public class StructuralStringAssert extends AbstractAssert<StructuralStringAssert, String> {

    private Tokenizer tokenizer = Tokenizer.JAVA_CODE;

    protected StructuralStringAssert(String actual) {
        super(actual, StructuralStringAssert.class);
    }

    /**
     * Entry point for structural assertions.
     */
    public static StructuralStringAssert assertThatStructurally(String actual) {
        return new StructuralStringAssert(actual);
    }

    /**
     * Sets the tokenizer to use for this assertion.
     */
    public StructuralStringAssert usingTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        return this;
    }

    /**
     * Asserts that the actual string is structurally equal to the expected string.
     * Uses a "smart diff" to preserve expected formatting in case of failure.
     */
    @Override
    public StructuralStringAssert isEqualTo(Object expected) {
        if (!(expected instanceof String)) {
            return super.isEqualTo(expected);
        }

        String expectedStr = (String) expected;
        String reformatted = LexicalAssert.reformatActual(tokenizer, expectedStr, actual);
        
        if (!expectedStr.equals(reformatted)) {
            // Throwing AssertionFailedError ensures IDEs show the side-by-side diff
            throw new AssertionFailedError(
                "Structural mismatch found",
                expectedStr,
                reformatted
            );
        }

        return this;
    }
}

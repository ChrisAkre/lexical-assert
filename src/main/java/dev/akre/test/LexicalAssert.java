package dev.akre.test;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import java.util.ArrayList;
import java.util.List;
import org.opentest4j.AssertionFailedError;

/**
 * Structural assertion utility that provides human-readable diffs for code generation tests.
 */
public class LexicalAssert {

    /**
     * Fluent AssertJ-style entry point for structural assertions.
     * Note: Requires AssertJ on the classpath.
     */
    public static StructuralStringAssert assertThatStructurally(String actual) {
        return StructuralStringAssert.assertThatStructurally(actual);
    }

    /**
     * Asserts that two strings are structurally equal based on a given tokenizer.
     * If they differ, an {@link AssertionFailedError} is thrown with a "smart" diff that
     * preserves the formatting of the expected string.
     *
     * @param tokenizer the tokenizer to use for defining structural tokens
     * @param expected  the expected source code (skeleton for the diff)
     * @param actual    the actual generated source code
     */
    public static void assertStructuralEquals(Tokenizer tokenizer, String expected, String actual) {
        String reformatted = reformatActual(tokenizer, expected, actual);
        if (!expected.equals(reformatted)) {
            throw new AssertionFailedError(null, expected, reformatted);
        }
    }

    /**
     * Reconstructs a "hybrid" version of the actual string by splicing the actual tokens
     * into the character offsets of the expected string.
     */
    static String reformatActual(Tokenizer tokenizer, String expected, String actual) {
        List<Tokenizer.Token> expectedTokens = tokenizer.tokenize(expected);
        List<Tokenizer.Token> actualTokens = tokenizer.tokenize(actual);

        // DiffUtils.diff uses equals() on the objects in the list.
        // Our Token implementations (SimpleToken and UnquotedCaseInsensitiveToken) 
        // provide the appropriate equality logic.
        Patch<Tokenizer.Token> patch = DiffUtils.diff(expectedTokens, actualTokens);
        if (patch.getDeltas().isEmpty()) {
            return expected;
        }

        StringBuilder hybrid = new StringBuilder(expected);
        List<AbstractDelta<Tokenizer.Token>> deltas = new ArrayList<>(patch.getDeltas());
        // Sort in reverse order of source position to maintain offset validity during reconstruction
        deltas.sort((a, b) -> Integer.compare(b.getSource().getPosition(), a.getSource().getPosition()));

        for (AbstractDelta<Tokenizer.Token> delta : deltas) {
            Chunk<Tokenizer.Token> source = delta.getSource();
            Chunk<Tokenizer.Token> target = delta.getTarget();

            int sourceStartOffset;
            int sourceEndOffset;

            if (source.getLines().isEmpty()) {
                // INSERT operation
                if (source.getPosition() < expectedTokens.size()) {
                    sourceStartOffset = expectedTokens.get(source.getPosition()).startOffset();
                } else {
                    sourceStartOffset = expected.length();
                }
                sourceEndOffset = sourceStartOffset;
            } else {
                // DELETE or CHANGE operation
                sourceStartOffset = expectedTokens.get(source.getPosition()).startOffset();
                sourceEndOffset = expectedTokens.get(source.getPosition() + source.size() - 1).endOffset();
            }

            String replacement = "";
            if (!target.getLines().isEmpty()) {
                int targetStartOffset = actualTokens.get(target.getPosition()).startOffset();
                int targetEndOffset = actualTokens.get(target.getPosition() + target.size() - 1).endOffset();
                replacement = actual.substring(targetStartOffset, targetEndOffset);
            }

            hybrid.replace(sourceStartOffset, sourceEndOffset, replacement);
        }

        return hybrid.toString();
    }
}

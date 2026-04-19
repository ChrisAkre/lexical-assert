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
            String replacement = "";

            if (source.getLines().isEmpty()) {
                // INSERT operation
                sourceStartOffset = source.getPosition() > 0
                        ? expectedTokens.get(source.getPosition() - 1).endOffset()
                        : 0;
                sourceEndOffset = source.getPosition() < expectedTokens.size()
                        ? expectedTokens.get(source.getPosition()).startOffset()
                        : expected.length();

                int targetStartOffset = target.getPosition() > 0
                        ? actualTokens.get(target.getPosition() - 1).endOffset()
                        : 0;
                int nextTargetPos = target.getPosition() + target.size();
                int targetEndOffset = nextTargetPos < actualTokens.size()
                        ? actualTokens.get(nextTargetPos).startOffset()
                        : actual.length();

                replacement = actual.substring(targetStartOffset, targetEndOffset);

                // --- THE MAGIC FIX: SMART NEWLINE PRESERVATION ---
                String expectedGap = expected.substring(sourceStartOffset, sourceEndOffset);
                if (expectedGap.contains("\n") && !replacement.contains("\n")) {
                    // Actual inserted text is on a single line, but expected skeleton has a newline.
                    // Strip trailing horizontal space from the replacement and append the expected gap (newline + any indent).
                    replacement = replacement.replaceAll("[ \\t]+$", "") + expectedGap;
                }

            } else {
                // DELETE or CHANGE operation
                // Strictly use token boundaries to preserve the expected string's formatting
                sourceStartOffset = expectedTokens.get(source.getPosition()).startOffset();
                sourceEndOffset = expectedTokens.get(source.getPosition() + source.size() - 1).endOffset();

                if (!target.getLines().isEmpty()) {
                    int targetStartOffset = actualTokens.get(target.getPosition()).startOffset();
                    int targetEndOffset = actualTokens.get(target.getPosition() + target.size() - 1).endOffset();
                    replacement = actual.substring(targetStartOffset, targetEndOffset);
                }
            }

            hybrid.replace(sourceStartOffset, sourceEndOffset, replacement);
        }

        return hybrid.toString();
    }
}

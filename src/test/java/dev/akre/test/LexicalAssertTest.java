package dev.akre.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class LexicalAssertTest {

    @Test
    public void testDecimal() {
        assertThrows(AssertionFailedError.class,
                () -> LexicalAssert.assertStructuralEquals(Tokenizer.JAVA_CODE, "2.5", "2 . 5"));

    }

    @Test
    public void testSuccessfulComparison() {
        String expected = "public   class  Foo  { }";
        String actual = "public class Foo {}";
        
        // Should not throw
        LexicalAssert.assertStructuralEquals(Tokenizer.JAVA_CODE, expected, actual);
    }

    @Test
    public void testFailingComparisonPreservesFormatting() {
        String expected = "public   class  Foo  { }";
        String actual = "public class Bar {}";

        AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> {
            LexicalAssert.assertStructuralEquals(Tokenizer.JAVA_CODE, expected, actual);
        });

        // The actual value in the error should have preserved the expected string's visual spacing
        // but swapped 'Foo' for 'Bar'
        assertEquals("public   class  Bar  { }", error.getActual().getValue());
        assertEquals(expected, error.getExpected().getValue());
    }

    @Test
    public void testReformatActual() {
        String expected = "a.b (  c  )";
        String actual = "a . b(c)";
        
        String reformatted = LexicalAssert.reformatActual(Tokenizer.JAVA_CODE, expected, actual);
        
        // Should match exactly because they are structurally identical
        assertEquals(expected, reformatted);
    }

    @Test
    public void testInsertion() {
        String expected = "[a c d]";
        String actual = "[a b c d]";
        String reformatted = LexicalAssert.reformatActual(Tokenizer.JAVA_CODE, expected, actual);

        assertEquals(actual, reformatted);
    }

    @Test
    public void testMultilineInsertion() {
        String expected = "SELECT id, phone\nFROM users";
        String actual = "SELECT id,phone, email FROM users";
        String reformatted = LexicalAssert.reformatActual(Tokenizer.JAVA_CODE, expected, actual);

        assertEquals("SELECT id, phone, email\nFROM users", reformatted);
    }


    @Test
    public void testAssertJFluentApi() {
        String actual = "select first,last from users";
        String expected = "SELECT\n" +
                          "  first,\n" +
                          "  last\n" +
                          "FROM\n" +
                          "  users";

        LexicalAssert.assertThatStructurally(actual)
                .usingTokenizer(Tokenizer.SQL)
                .isEqualTo(expected);
    }
}

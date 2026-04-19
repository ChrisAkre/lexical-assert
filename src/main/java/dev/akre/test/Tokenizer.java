package dev.akre.test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface for breaking text into semantic tokens with offset tracking.
 */
public interface Tokenizer {
    interface TokenFactory {
        Token create(String value, int start, int end);
    }

    List<Token> tokenize(String text);

    static Tokenizer ofRegex(String regex) {
        return ofRegex(Pattern.compile(regex));
    }

    static Tokenizer ofRegex(Pattern regex) {
        return ofRegex(regex, SimpleToken::new);
    }

    static Tokenizer ofRegexUnquotedCaseInsensitive(String regex) {
        return ofRegexUnquotedCaseInsensitive(Pattern.compile(regex));
    }

    static Tokenizer ofRegexUnquotedCaseInsensitive(Pattern regex) {
        return ofRegex(regex, UnquotedCaseInsensitiveToken::new);
    }

    static Tokenizer ofRegex(Pattern regex, TokenFactory factory) {
        return s -> {
            List<Token> tokens = new ArrayList<>();
            Matcher m = regex.matcher(s);
            while (m.find()) {
                tokens.add(factory.create(m.group(), m.start(), m.end()));
            }
            return tokens;
        };
    }

    /**
     * Represents a single semantic token with its position in the source text.
     */
    abstract class Token implements Comparable<Token> {
        private final String text;
        private final int startOffset;
        private final int endOffset;

        public Token(String text, int startOffset, int endOffset) {
            this.text = text;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        public String text() { return text; }
        public int startOffset() { return startOffset; }
        public int endOffset() { return endOffset; }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof Token && this.compareTo((Token) other) == 0);
        }

        @Override
        public int hashCode() {
            return text.toLowerCase().hashCode();
        }

        @Override
        public String toString() { return text; }
    }

    /**
     * A standard case-sensitive token.
     */
    class SimpleToken extends Token {
        public SimpleToken(String text, int startOffset, int endOffset) {
            super(text, startOffset, endOffset);
        }

        @Override
        public int compareTo(Token other) {
            return this.text().compareTo(other.text());
        }
    }

    /**
     * A token that performs case-insensitive comparison for unquoted text.
     */
    class UnquotedCaseInsensitiveToken extends Token {
        public UnquotedCaseInsensitiveToken(String text, int startOffset, int endOffset) {
            super(text, startOffset, endOffset);
        }

        @Override
        public int compareTo(Token other) {
            if (quoted() || (other instanceof UnquotedCaseInsensitiveToken && ((UnquotedCaseInsensitiveToken) other).quoted())) {
                return this.text().compareTo(other.text());
            }
            return this.text().compareToIgnoreCase(other.text());
        }

        boolean quoted() {
            String t = text();
            return t.startsWith("'") || t.startsWith("\"");
        }
    }

    /**
     * A standard tokenizer for Java-like source code.
     */
    Tokenizer JAVA_CODE = Tokenizer.ofRegex(
            Pattern.compile("(?x)\n" +
            "\"(?:\\\\\\\\\"|[^\"])*\"     # 1. Strings (Double quotes, allowing escaped \\\")\n" +
            "|                     # OR\n" +
            "'(?:\\\\\\\\'|[^'])*'     # 2. Characters (Single quotes, allowing escaped \\\')\n" +
            "|                     # OR\n" +
            "[a-zA-Z0-9_$]+        # 3. Words (Identifiers, Numbers, Keywords)\n" +
            "|                     # OR\n" +
            "[^a-zA-Z0-9_$\\s]     # 4. Symbols (Everything else except whitespace)")
    );

    /**
     * A tokenizer for JSON data.
     */
    Tokenizer JSON = Tokenizer.ofRegex(
            Pattern.compile("(?x)\n" +
            "\"(?:\\\\\\\\\"|[^\"])*\"     # 1. Strings\n" +
            "|                     # OR\n" +
            "[a-zA-Z0-9_]+         # 2. Words (Integers, true, false, null)\n" +
            "|                     # OR\n" +
            "[^a-zA-Z0-9_\\s]       # 3. Symbols ({}, [], :, ,, ., -)")
    );

    /**
     * A tokenizer for ANSI SQL.
     */
    Tokenizer SQL = Tokenizer.ofRegexUnquotedCaseInsensitive(
            Pattern.compile("(?x)\n" +
            "'(?:''|[^'])*'        # 1. Strings (Single quotes, escaped via '')\n" +
            "|                     # OR\n" +
            "\"(?:\"\"|[^\"])*\"     # 2. Quoted Identifiers (Double quotes)\n" +
            "|                     # OR\n" +
            "[a-zA-Z0-9_]+         # 3. Words (Identifiers, Numbers, Keywords)\n" +
            "|                     # OR\n" +
            "[^a-zA-Z0-9_\\s]       # 4. Symbols (Operators, Punctuation)")
    );

    /**
     * A tokenizer for ANSI SQL that ignores comments.
     */
    Tokenizer SQL_IGNORING_COMMENTS = s -> {
        List<Token> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("(?x)\n" +
                "--[^\\n]*              # Single-line comment\n" +
                "|                     # OR\n" +
                "/\\*[\\s\\S]*?\\*/        # Multi-line comment\n" +
                "|                     # OR\n" +
                "'(?:''|[^'])*'        # 1. Strings (Single quotes, escaped via '')\n" +
                "|                     # OR\n" +
                "\"(?:\"\"|[^\"])*\"     # 2. Quoted Identifiers (Double quotes)\n" +
                "|                     # OR\n" +
                "[a-zA-Z0-9_]+         # 3. Words (Identifiers, Numbers, Keywords)\n" +
                "|                     # OR\n" +
                "[^a-zA-Z0-9_\\s]       # 4. Symbols (Operators, Punctuation)").matcher(s);
        while (m.find()) {
            String group = m.group();
            if (group.startsWith("--") || group.startsWith("/*")) {
                continue;
            }
            tokens.add(new UnquotedCaseInsensitiveToken(group, m.start(), m.end()));
        }
        return tokens;
    };
}

package dev.akre.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class LexicalAssertHarnessTest {

    private static final Path RESOURCES = Paths.get("src/test/resources");

    @TestFactory
    public Stream<DynamicNode> lexicalAssertTests() throws IOException {
        return Stream.of("java", "json", "sql", "sql_with_comments")
                .map(RESOURCES::resolve)
                .filter(Files::exists)
                .flatMap(this::processLanguageDir);
    }

    private Stream<DynamicNode> processLanguageDir(Path langDir) {
        try {
            final Tokenizer tokenizer = getTokenizer(langDir.getFileName().toString());
            return Files.list(langDir)
                    .filter(p -> p.toString().endsWith(".expected." + langDir.getFileName().toString()))
                    .map(expectedPath -> processExpectedFile(tokenizer, expectedPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DynamicNode processExpectedFile(final Tokenizer tokenizer, final Path expectedPath) {
        String fileName = expectedPath.getFileName().toString();
        final String testName = fileName.substring(0, fileName.indexOf(".expected."));
        final String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        
        try {
            final String expectedContent = new String(Files.readAllBytes(expectedPath));
            
            Stream<DynamicTest> failingTests = Files.list(expectedPath.getParent())
                    .filter(p -> p.getFileName().toString().startsWith(testName + ".") && p.getFileName().toString().endsWith(".failing." + extension))
                    .map(failingPath -> {
                        String fName = failingPath.getFileName().toString();
                        final String caseName = fName.substring(testName.length() + 1, fName.indexOf(".failing."));
                        final Path reformattedPath = failingPath.getParent().resolve(testName + "." + caseName + ".reformatted." + extension);
                        
                        return DynamicTest.dynamicTest(testName + " [fail: " + caseName + "]", () -> {
                            String actualContent = new String(Files.readAllBytes(failingPath));
                            String expectedReformatted = new String(Files.readAllBytes(reformattedPath));
                            
                            String actualReformatted = LexicalAssert.reformatActual(tokenizer, expectedContent, actualContent);
                            assertEquals(expectedReformatted, actualReformatted, "Reformatted output mismatch for case: " + caseName);
                        });
                    });

            Stream<DynamicTest> matchingTests = Files.list(expectedPath.getParent())
                    .filter(p -> p.getFileName().toString().startsWith(testName + ".") && p.getFileName().toString().endsWith(".matching." + extension))
                    .map(matchingPath -> {
                        String mName = matchingPath.getFileName().toString();
                        final String caseName = mName.substring(testName.length() + 1, mName.indexOf(".matching."));
                        
                        return DynamicTest.dynamicTest(testName + " [match: " + caseName + "]", () -> {
                            String actualContent = new String(Files.readAllBytes(matchingPath));
                            // This should not throw an exception
                            LexicalAssert.assertStructuralEquals(tokenizer, expectedContent, actualContent);
                        });
                    });

            DynamicTest successTest = DynamicTest.dynamicTest(testName + " [self-match]", () -> {
                String actualReformatted = LexicalAssert.reformatActual(tokenizer, expectedContent, expectedContent);
                assertEquals(expectedContent, actualReformatted, "Self-match should return original expected content");
            });

            return DynamicContainer.dynamicContainer(testName, Stream.concat(Stream.of(successTest), Stream.concat(failingTests, matchingTests)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Tokenizer getTokenizer(String lang) {
        switch (lang) {
            case "java": return Tokenizer.JAVA_CODE;
            case "json": return Tokenizer.JSON;
            case "sql": return Tokenizer.SQL;
            case "sql_with_comments": return Tokenizer.SQL_IGNORING_COMMENTS;
            default: throw new IllegalArgumentException("Unknown language: " + lang);
        }
    }
}

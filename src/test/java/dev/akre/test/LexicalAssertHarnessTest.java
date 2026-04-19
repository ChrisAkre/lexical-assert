package dev.akre.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class LexicalAssertHarnessTest {

    private static final Path RESOURCES = Paths.get("src/test/resources");


    private static class ParsedFile {
        final String name;
        final String description;
        final String content;

        ParsedFile(String name, String description, String content) {
            this.name = name;
            this.description = description;
            this.content = content;
        }
    }

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\r?\\n(.*?)\\r?\\n---\\r?\\n(.*)$", Pattern.DOTALL);

    private ParsedFile parseFile(Path path, String defaultName, boolean isExpected) throws IOException {
        String rawContent = new String(Files.readAllBytes(path));
        Matcher matcher = FRONTMATTER_PATTERN.matcher(rawContent);

        String name = defaultName;
        String description = null;
        String content = rawContent;

        if (matcher.find()) {
            String yamlStr = matcher.group(1);
            content = matcher.group(2);
            YamlMapping yaml = Yaml.createYamlInput(yamlStr).readYamlMapping();

            String nameKey = isExpected ? "test-name" : "case-name";
            if (yaml.string(nameKey) != null) {
                name = yaml.string(nameKey);
            }
            if (yaml.string("description") != null) {
                description = yaml.string("description");
            }
        }
        return new ParsedFile(name, description, content);
    }


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
        final String fileTestName = fileName.substring(0, fileName.indexOf(".expected."));
        final String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        
        try {
            ParsedFile parsedExpected = parseFile(expectedPath, fileTestName, true);
            final String testName = parsedExpected.name;
            final String expectedContent = parsedExpected.content;
            
            Stream<DynamicTest> failingTests = Files.list(expectedPath.getParent())
                    .filter(p -> p.getFileName().toString().startsWith(fileTestName + ".") && p.getFileName().toString().endsWith(".failing." + extension))
                    .map(failingPath -> {
                        String fName = failingPath.getFileName().toString();
                        final String defaultCaseName = fName.substring(fileTestName.length() + 1, fName.indexOf(".failing."));
                        final Path reformattedPath = failingPath.getParent().resolve(fileTestName + "." + defaultCaseName + ".reformatted." + extension);
                        
                        ParsedFile parsedFailing;
                        try {
                            parsedFailing = parseFile(failingPath, defaultCaseName, false);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        return DynamicTest.dynamicTest(testName + " [fail: " + parsedFailing.name + "]", failingPath.toUri(), () -> {
                            String actualContent = parsedFailing.content;
                            String expectedReformatted = new String(Files.readAllBytes(reformattedPath));
                            
                            String actualReformatted = LexicalAssert.reformatActual(tokenizer, expectedContent, actualContent);
                            assertEquals(expectedReformatted, actualReformatted, "Reformatted output mismatch for case: " + parsedFailing.name);
                        });
                    });

            Stream<DynamicTest> matchingTests = Files.list(expectedPath.getParent())
                    .filter(p -> p.getFileName().toString().startsWith(fileTestName + ".") && p.getFileName().toString().endsWith(".matching." + extension))
                    .map(matchingPath -> {
                        String mName = matchingPath.getFileName().toString();
                        final String defaultCaseName = mName.substring(fileTestName.length() + 1, mName.indexOf(".matching."));

                        ParsedFile parsedMatching;
                        try {
                            parsedMatching = parseFile(matchingPath, defaultCaseName, false);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        
                        return DynamicTest.dynamicTest(testName + " [match: " + parsedMatching.name + "]", matchingPath.toUri(), () -> {
                            String actualContent = parsedMatching.content;
                            // This should not throw an exception
                            LexicalAssert.assertStructuralEquals(tokenizer, expectedContent, actualContent);
                        });
                    });

            DynamicTest successTest = DynamicTest.dynamicTest(testName + " [self-match]", expectedPath.toUri(), () -> {
                String actualReformatted = LexicalAssert.reformatActual(tokenizer, expectedContent, expectedContent);
                assertEquals(expectedContent, actualReformatted, "Self-match should return original expected content");
            });

            return DynamicContainer.dynamicContainer(testName, expectedPath.toUri(), Stream.concat(Stream.of(successTest), Stream.concat(failingTests, matchingTests)));

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

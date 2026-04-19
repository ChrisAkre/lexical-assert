import re

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "r") as f:
    content = f.read()

# We need to change the test name string in DynamicTest.dynamicTest for failing and matching tests
# to use the dynamically parsed name, but since the parsed name is only known inside the lambda,
# and DynamicTest.dynamicTest requires the name eagerly, we should parse the file *outside* the lambda
# to get the test name, but we shouldn't fail if we throw IOException.

new_process = """
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
"""

content = re.sub(r'private DynamicNode processExpectedFile.*?\} catch \(IOException e\) \{\s+throw new RuntimeException\(e\);\s+\}\s+\}', new_process.strip(), content, flags=re.DOTALL)

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "w") as f:
    f.write(content)

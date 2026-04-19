import re

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "r") as f:
    content = f.read()

# Replace processExpectedFile
old_process = """
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
"""

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

                        return DynamicTest.dynamicTest(testName + " [fail: " + defaultCaseName + "]", failingPath.toUri(), () -> {
                            ParsedFile parsedFailing = parseFile(failingPath, defaultCaseName, false);
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

                        return DynamicTest.dynamicTest(testName + " [match: " + defaultCaseName + "]", matchingPath.toUri(), () -> {
                            ParsedFile parsedMatching = parseFile(matchingPath, defaultCaseName, false);
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

content = content.replace(old_process.strip(), new_process.strip())

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "w") as f:
    f.write(content)

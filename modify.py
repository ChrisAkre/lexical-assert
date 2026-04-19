import re

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "r") as f:
    content = f.read()

imports = """
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
"""

content = content.replace("import java.util.stream.Stream;", "import java.util.stream.Stream;\n" + imports)

parsed_file_class = """
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

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\n(.*?)\\n---\\n(.*)$", Pattern.DOTALL);

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
"""

content = content.replace("private static final Path RESOURCES = Paths.get(\"src/test/resources\");",
                          "private static final Path RESOURCES = Paths.get(\"src/test/resources\");\n\n" + parsed_file_class)

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "w") as f:
    f.write(content)

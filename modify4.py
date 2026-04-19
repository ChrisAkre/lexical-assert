import re

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "r") as f:
    content = f.read()

content = content.replace('Pattern.compile("^---\\n(.*?)\\n---\\n(.*)$", Pattern.DOTALL)', 'Pattern.compile("^---\\\\r?\\\\n(.*?)\\\\r?\\\\n---\\\\r?\\\\n(.*)$", Pattern.DOTALL)')

with open("src/test/java/dev/akre/test/LexicalAssertHarnessTest.java", "w") as f:
    f.write(content)

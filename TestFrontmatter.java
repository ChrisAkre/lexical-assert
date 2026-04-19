import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestFrontmatter {
    public static void main(String[] args) throws Exception {
        String content = "---\ntest-name: My Test\ndescription: Some desc\n---\nReal content here\nand here.";
        Pattern p = Pattern.compile("^---\n(.*?)\n---\n(.*)$", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (m.find()) {
            System.out.println("YAML:\n" + m.group(1));
            System.out.println("Content:\n" + m.group(2));
            YamlMapping yaml = Yaml.createYamlInput(m.group(1)).readYamlMapping();
            System.out.println("test-name: " + yaml.string("test-name"));
        } else {
            System.out.println("No match");
        }
    }
}

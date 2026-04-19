import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

public class TestYaml {
    public static void main(String[] args) throws Exception {
        String yamlStr = "test-name: My Test\ndescription: Some desc";
        YamlMapping yaml = Yaml.createYamlInput(yamlStr).readYamlMapping();
        System.out.println("test-name: " + yaml.string("test-name"));
        System.out.println("description: " + yaml.string("description"));
        System.out.println("other: " + yaml.string("other"));
    }
}

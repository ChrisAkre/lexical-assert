import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.DynamicContainer;
import java.net.URI;
import java.util.stream.Stream;

public class TestJunit2 {
    public static void main(String[] args) throws Exception {
        URI uri = new URI("file:///tmp/test.txt");
        DynamicTest test = DynamicTest.dynamicTest("name", uri, () -> {});
        DynamicContainer container = DynamicContainer.dynamicContainer("name", uri, Stream.empty());
        System.out.println("Success");
    }
}

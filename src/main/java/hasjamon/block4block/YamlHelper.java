package hasjamon.block4block;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class YamlHelper {

    public static void generateConfigYaml(Map<String, Object> config, String filePath) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(filePath)) {
            yaml.dump(config, writer);
        }
    }
}
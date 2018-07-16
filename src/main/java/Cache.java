import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class Cache {

    private static String jsonData = App.readFile("./cache.json");
    private static JSONObject configObject = new JSONObject(jsonData);

    public String getValue(String key) {
        return configObject == null ? "" : configObject.get(key).toString();
    }

    public boolean getBolValue(String key) {
        return configObject.getBoolean(key);
    }

    public void setValue(String key, String value) {
        JSONObject object = configObject;
        object.put(key, value);

        try (FileWriter file = new FileWriter("./cache.json")) {
            file.write(object.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

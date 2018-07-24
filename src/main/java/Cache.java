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
}

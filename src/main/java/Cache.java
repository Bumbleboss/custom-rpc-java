import org.json.JSONObject;
import java.io.*;
import java.util.Objects;

public class Cache {

    private static final String jsonData = readFile(App.cache.getName());
    public static JSONObject configObject = new JSONObject(Objects.requireNonNull(jsonData));

    private static String readFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(fileName);
            try(BufferedReader br = new BufferedReader(fileReader)) {
                for(String line; (line = br.readLine()) != null; ) {
                    sb.append(line).append("\n");
                }
            }
            fileReader.close();
            return sb.toString();
        }catch (Exception ex) {
            if(ex instanceof FileNotFoundException) {
                return "File was not found";
            }else{
                App.logger.error(ex.getMessage(), ex);
                Windows.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true, true);
                System.exit(0);
                return null;
            }
        }
    }
}

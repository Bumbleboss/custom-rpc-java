import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import javafx.scene.control.TextField;

@SuppressWarnings("WeakerAccess")
public class CacheHandler {

    private static final File cache = App.cache;
    private static final TextField appId = App.clientText;
    private static final TextField details = App.detailsText;
    private static final TextField state = App.stateText;
    private static final TextField lrgKey = App.lrgImgKey;
    private static final TextField lrgTxt = App.lrgImgTxt;
    private static final TextField smlKey = App.smlImgKey;
    private static final TextField smlTxt = App.smlImgTxt;
    private static final CheckBox timestamp = App.box;
    private static final ChoiceBox<String> drpDown = App.drpDown;


    public static void setSettings(int i) {
        if(cache.exists()) {
            JSONObject app = Objects.requireNonNull(Cache.configObject.getJSONArray("apps")).getJSONObject(i);
            if(app==null) {
                return;
            }
            try {
                appId.setText(app.getString("clientId"));
                details.setText(app.getString("details"));
                state.setText(app.getString("state"));
                timestamp.setSelected(app.getBoolean("timestamp"));
                lrgKey.setText(app.getString("lrgKey"));
                lrgTxt.setText(app.getString("lrgTxt"));
                smlKey.setText(app.getString("smlKey"));
                smlTxt.setText(app.getString("smlTxt"));
            } catch (JSONException ex) {
                Windows.display("An error has occured", ex.getMessage()+ "\nPlease check if there's any wrong inputs with the cache file and try again", true, true);
                System.exit(0);
            }
        }
    }

    public static int getSelection() {
        String value = drpDown.getValue()==null?"FIRST":drpDown.getValue();
        switch (value) {
            case "FIRST":
                return 0;
            case "SECOND":
                return 1;
            case "THIRD":
                return 2;
            case "FOURTH":
                return 3;
            case "FIFTH":
                return 4;
        }
        return 0;
    }

    private static void writeSettings(int i) {
        JSONObject obj = new JSONObject()
                .put("clientId", appId.getText()).put("details", details.getText())
                .put("state", state.getText()).put("timestamp", timestamp.isSelected())
                .put("lrgKey", lrgKey.getText()).put("lrgTxt", lrgTxt.getText())
                .put("smlKey", smlKey.getText()).put("smlTxt", smlTxt.getText());

        if(!cache.exists()) {
            String json = new JSONObject().put("apps", new JSONArray().put(i, obj)).toString();
            writeFile(cache.getName(), json);
        }else{
            JSONArray json = Objects.requireNonNull(Cache.configObject.getJSONArray("apps")).put(i, obj);
            writeFile(cache.getName(), new JSONObject().put("apps", json).toString());
        }
    }

    public static void updateCache(String oldData, String newData, boolean ask) {
        if(!cache.exists()) {
            if(ask) {
                if(Windows.display("Info", "Do you want to save your input data?", false, true)) {
                    App.logger.info("Created cache file!");
                    CacheHandler.writeSettings(CacheHandler.getSelection());
                    App.logger.info("Written input data to the cahce file!");
                }
            }
        }else{
            CacheHandler.writeSettings(CacheHandler.getSelection());
            if(oldData.isEmpty() && newData.isEmpty()) {
                App.logger.info("Written full new input data to cache file before closing");
                return;
            }
            App.logger.debug("Written new input data to the cache file! Choice: " + (CacheHandler.getSelection() + 1) + " with input data from {" + oldData + "} to {" + newData+"}");
        }
    }

    public static boolean isOldCache() {
        if(cache.exists()) {
            return Cache.configObject.has("clientId");
        }
        return false;
    }

    public static void updateOldToNewCache() {
        App.logger.info("Containing old cache file, updating it to the new version");
        JSONObject oldObj = Cache.configObject;
        JSONObject obj = new JSONObject()
                .put("clientId", oldObj.getString("clientId")).put("details", oldObj.getString("details"))
                .put("state", oldObj.getString("state")).put("timestamp", oldObj.getBoolean("timestamp"))
                .put("lrgKey", oldObj.getString("lrgKey")).put("lrgTxt", oldObj.getString("lrgTxt"))
                .put("smlKey", oldObj.getString("smlKey")).put("smlTxt", oldObj.getString("smlTxt"));
        String json = new JSONObject().put("apps", new JSONArray().put(0, obj)).toString();
        writeFile(cache.getName(), json);
        App.logger.info("Successfully updated from old cache layout to the newest");
    }

    private static void writeFile(String fileName, String body) {
        byte data[] = body.getBytes();
        try {
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(data);
            out.close();
        }catch (IOException ex) {
            App.logger.error(ex.getMessage(), ex);
            Windows.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true, false);
            System.exit(0);
        }
    }
}

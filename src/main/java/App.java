import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;


public class App extends Application {

    private Stage window;
    private final File cache = new File("cache.json");
    private static String appName = "custom-rpc-java";
    private static double appVersion = 0.6;
    private static Logger logger = LoggerFactory.getLogger(appName);
    private static final String icon = App.class.getResource("256x256.png").toExternalForm().replace("20%", " ");
    private static final String strayIcon = App.class.getResource("16x16.png").toExternalForm().replace("20%", " ");
    private static final String font = App.class.getResource("TipoType_Brother_1816_Medium.otf").toExternalForm().replace("%20", " ");

    //TEXT FIELDS & CHECKBOX
    private final TextField clientText = getTextField(1);
    private final TextField detailsText = getTextField(3);
    private final TextField stateText = getTextField(5);
    private final TextField lrgImgKey = getTextField(9);
    private final TextField smlImgKey = getTextField(11);
    private final TextField lrgImgTxt = getTextField(13);
    private final TextField smlImgTxt = getTextField(15);
    private final Label enTime = getLabel("ENABLE TIME", 6);
    private final CheckBox box = new CheckBox();

    public static void main(String[] args) {
        Font.loadFont(font, 0);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet("style.css");
        try {
            if(!isLatest()) {
                boolean answer = Confirm.display("New update " + getLatestVersion(), "What's new:\n"+getNewFeatures()+"\n\nDo you want to download it?", false);
                if(answer) {
                    URL url = new URL(getDownloadLink());
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(getJarName());
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    logger.info("Downloaded update version " + getLatestVersion());
                    rbc.close();
                    fos.close();
                    Confirm.display("Download complete", "Update is now downloaded. Application will now close", true);
                    System.exit(0);
                }
            }

            window = primaryStage;
            window.setTitle(appName+" v"+appVersion);

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(10, 10, 10, 10));
            grid.setVgap(8);
            grid.setHgap(10);

            //DISCORD RPC
            DiscordRPC lib = DiscordRPC.INSTANCE;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            handlers.ready = (user) -> logger.info("Presence has successfully updated! Logged on "+ user.username+"#"+user.discriminator + "("+user.userId+")");
            handlers.disconnected = (code, message) -> logger.info(code + " - " + message);
            handlers.errored = (code, message) -> logger.warn(code + " - " + message);

            //CHECKBOX
            box.setPrefSize(17, 17);
            enTime.setPadding(new Insets(0, 0, 0, 30));
            GridPane.setConstraints(box, 0, 6);

            //BUTTONS
            Button upd = getButton("UPDATE", 0);
            Button shut = getButton("SHUTDOWN", 187);
            Button prev = getButton("PREVIEW", 400);

            //ENABLES BUTTON AFTER USER INPUT
            BooleanBinding booleanBind = clientText.textProperty().isEmpty();
            upd.disableProperty().bind(booleanBind);


            logger.info("Running callbacks");
            Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    lib.Discord_RunCallbacks();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        lib.Discord_Shutdown();
                        break;
                    }
                }
            }, "RPC-Callback-Handler");
            thread.start();

            if(cache.exists()) {
                Cache cacheSet = new Cache();
                clientText.setText(cacheSet.getValue("clientId"));
                detailsText.setText(cacheSet.getValue("details"));
                stateText.setText(cacheSet.getValue("state"));
                box.setSelected(cacheSet.getBolValue("timestamp"));
                lrgImgKey.setText(cacheSet.getValue("lrgKey"));
                lrgImgTxt.setText(cacheSet.getValue("lrgTxt"));
                smlImgKey.setText(cacheSet.getValue("smlKey"));
                smlImgTxt.setText(cacheSet.getValue("smlTxt"));
            }


            //UPDATE BUTTON
            upd.setOnAction(e -> {
                String appId = clientText.getText();
                String details = detailsText.getText();
                String state = stateText.getText();
                String lrgKey = lrgImgKey.getText();
                String lrgTxt = lrgImgTxt.getText();
                String smlKey = smlImgKey.getText();
                String smlTxt = smlImgTxt.getText();

                lib.Discord_Initialize(appId, handlers, true, "");
                DiscordRichPresence presence = new DiscordRichPresence();

                if (box.isSelected()) {
                    presence.startTimestamp = System.currentTimeMillis() / 1000; // epoch second
                }

                presence.details = details.isEmpty() ? "" : details;
                presence.state = state.isEmpty() ? "" : state;
                presence.largeImageKey = lrgKey.isEmpty() ? "" : lrgKey;
                presence.largeImageText = lrgTxt.isEmpty() ? "" : lrgTxt;
                presence.smallImageKey = smlKey.isEmpty() ? "" : smlKey;
                presence.smallImageText = smlTxt.isEmpty() ? "" : smlTxt;

                lib.Discord_UpdatePresence(presence);
            });

            //SHUTDOWN PROCESS
            window.setOnCloseRequest(e -> closeApp(thread));
            shut.setOnAction(e -> closeApp(thread));

            //IS DISABLED TILL ITS FUNCTION IS MADE
            prev.setDisable(true);

            //ADDING THE LOVELY STUFF
            grid.getChildren().addAll(
                    getLabel("CLIENT ID*", 0), clientText,
                    getLabel("DETAILS", 2), detailsText,
                    getLabel("STATE", 4), stateText,
                    enTime, box,
                    getLabel("LARGE IMAGE KEY", 8), lrgImgKey,
                    getLabel("SMALL IMAGE KEY", 10), smlImgKey,
                    getLabel("LARGE IMAGE TEXT", 12), lrgImgTxt,
                    getLabel("SMALL IMAGE TEXT", 14), smlImgTxt,
                    upd, shut, prev
            );

            //SYSTEM TRAY FUNCTION
            window.iconifiedProperty().addListener((prop, oldValue, newValue) -> {
                if (newValue) {
                    if (!SystemTray.isSupported()) {
                        logger.error("SystemTray is not supported");
                        Confirm.display("An error has occured", "The OS you are using does not support System tray",true);
                        return;
                    }

                    window.hide();
                    final PopupMenu popup = new PopupMenu();
                    final TrayIcon trayIcon = new TrayIcon(getStrayIcon());
                    final SystemTray tray = SystemTray.getSystemTray();

                    MenuItem showApp = new MenuItem("Show application");
                    MenuItem exitBtn = new MenuItem("Exit");

                    exitBtn.addActionListener(es -> Platform.runLater(() -> closeApp(thread)));

                    showApp.addActionListener(es -> Platform.runLater(() -> {
                        showWindow();
                        tray.remove(trayIcon);
                    }));

                    trayIcon.addActionListener(es -> Platform.runLater(() ->{
                        showWindow();
                        tray.remove(trayIcon);
                    }));

                    popup.add(showApp);
                    popup.add(exitBtn);
                    trayIcon.setPopupMenu(popup);

                    try {
                        tray.add(trayIcon);
                    } catch (AWTException ex) {
                        logger.error("TrayIcon could not be added", ex);
                        Confirm.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true);
                        System.exit(0);
                    }
                    trayIcon.setToolTip(appName);
                    trayIcon.displayMessage("Info!", appName +" is now running in background",  TrayIcon.MessageType.INFO);
                }
            });

            Scene scene = new Scene(grid, 620, 700);
            window.getIcons().add(new javafx.scene.image.Image(icon));
            window.setScene(scene);
            window.setResizable(false);
            window.show();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Confirm.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true);
            System.exit(0);
        }
    }

    private Label getLabel(String text, int row) {
        Label label = new Label(text);
        GridPane.setConstraints(label, 0, row);
        return label;
    }

    private TextField getTextField(int row) {
        TextField field = new TextField();
        field.setPrefSize(470, 25);
        GridPane.setConstraints(field, 0, row);
        return field;
    }

    private Button getButton(String text, int left) {
        Button btn = new Button(text);
        btn.setPadding(new Insets(10, 30, 10, 30));
        GridPane.setMargin(btn, new Insets(20,0,0, left));
        GridPane.setConstraints(btn, 0, 16);
        return btn;
    }

    private void closeApp(Thread thread) {
        if(window.isIconified()) {
           showWindow();
        }
        if(!cache.exists()) {
            if(Confirm.display("Confirm","Do you want to save your input data?", false)) {
                logger.info("Created cache file!");
                writeCache();
                logger.info("Written input data to the cahce file!");
            }
        }else{
            writeCache();
        }

        logger.info("Shutting down DiscordRPC library");
        thread.interrupt();
        logger.info("Closing application");
        System.exit(0);
    }

    public static String readFile(String fileName) {
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
                logger.error(ex.getMessage(), ex);
                Confirm.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true);
                System.exit(0);
                return null;
            }
        }
    }

    private void writeFile(String fileName, String body) {
        byte data[] = body.getBytes();
        try {
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(data);
            out.close();
        }catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            Confirm.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true);
            System.exit(0);
        }
    }

    private void writeCache() {
        String appId = clientText.getText();
        String details = detailsText.getText();
        String state = stateText.getText();
        String lrgKey = lrgImgKey.getText();
        String lrgTxt = lrgImgTxt.getText();
        String smlKey = smlImgKey.getText();
        String smlTxt = smlImgTxt.getText();

        String json = new JSONObject().put("clientId", appId).put("details", details)
                .put("state", state).put("timestamp", box.isSelected())
                .put("lrgKey", lrgKey).put("lrgTxt", lrgTxt).put("smlKey", smlKey)
                .put("smlTxt", smlTxt).toString();
        writeFile(cache.getName(), json);
    }

    private void showWindow () {
        if(window != null) {
            window.setIconified(false);
            window.show();
            window.toFront();
        }
    }

    private Image getStrayIcon() {
        URL url = null;
        try {
            url = new URL(strayIcon);
        } catch (MalformedURLException ex) {
            logger.error("importing stray icon went through an error", ex);
            Confirm.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true);
            System.exit(0);
        }
        return Toolkit.getDefaultToolkit().getImage(url);
    }

    private  String body = requestGET("https://api.github.com/repos/Bumbleboss/"+App.appName+"/releases/latest");
    private JSONObject json = new JSONObject(Objects.requireNonNull(body));

    private boolean isLatest() {
        return appVersion >= getLatestVersion();
    }

    private double getLatestVersion() {
        String ver = json.getString("tag_name");
        return Double.parseDouble(ver);
    }

    private String getDownloadLink() {
        return json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
    }

    private String getJarName() {
        return json.getJSONArray("assets").getJSONObject(0).getString("name");
    }

    private String getNewFeatures() {
        return json.getString("body");
    }

    private String requestGET(String link) {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(link);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            return result.toString();
        } catch (Exception ex) {
            App.logger.error("Something happened while trying to check for a new version", ex);
            Confirm.display("An error has occured", "Something happened while trying to check for a new version, please restart application.\nIf the problem still arises, please report it to the github page.",true);
            System.exit(0);
            return null;
        }
    }
}

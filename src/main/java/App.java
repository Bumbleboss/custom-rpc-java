import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.awt.MenuItem;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;


public class App extends Application {

    private Stage window;
    public static final File cache = new File("cache.json");
    private static final String appName = "custom-rpc-java";
    private static final double appVersion = 0.7;
    public static final Logger logger = LoggerFactory.getLogger(appName);
    private static final String icon = App.class.getResource("256x256.png").toExternalForm().replace("20%", " ");
    private static final String strayIcon = App.class.getResource("16x16.png").toExternalForm().replace("20%", " ");
    private static final String font = App.class.getResource("TipoType_Brother_1816_Medium.otf").toExternalForm().replace("%20", " ");

    //WINDOW ELEMENTS
    public static final TextField clientText = getTextField(1);
    public static final TextField detailsText = getTextField(3);
    public static final TextField stateText = getTextField(5);
    public static final TextField lrgImgKey = getTextField(9);
    public static final TextField smlImgKey = getTextField(11);
    public static final TextField lrgImgTxt = getTextField(13);
    public static final TextField smlImgTxt = getTextField(15);
    private static final Label enTime = getLabel("ENABLE TIME", 6);
    public static final CheckBox box = new CheckBox();
    public static final ChoiceBox<String> drpDown = new ChoiceBox<>();

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
                boolean answer = Windows.display("New update " + getLatestVersion(), "What's new:\n"+getNewFeatures()+"\n\nDo you want to download it?", false, true);
                if(answer) {
                    Windows.display("Downloading...", "Please wait while the download finishes...", true, false);
                    logger.info("Started downloading the jar file");
                    URL url = new URL(getDownloadLink());
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(getJarName());
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    logger.info("Downloaded update version " + getLatestVersion());
                    rbc.close();
                    fos.close();
                    Windows.display("Download complete", "Update is now downloaded. Application will now close", true, true);
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

            //LAST MENU
            Button upd = getButton("UPDATE", 0);
            Button shut = getButton("SHUTDOWN", 150);

            GridPane.setMargin(drpDown, new Insets(20, 0, 0, 335));
            GridPane.setConstraints(drpDown, 0, 16);

            drpDown.getItems().addAll("FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH");

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

            //SETTINGS UPDATED TO ELEMENTS IF CACHE EXITS
            if(CacheHandler.isOldCache()) {
                CacheHandler.updateOldToNewCache();
                Windows.display("Info", "Old cache layout. New layout has been \nupdated for the cache file. Please re-open app", true, true);
                System.exit(0);
            }
            CacheHandler.setSettings(CacheHandler.getSelection());

            //UPDATERS FOR WHEN A CHANGE HAPPENS TO ANY OF THE ELEMENTS
            clientText.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            detailsText.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            stateText.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            lrgImgKey.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            lrgImgTxt.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            smlImgKey.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            smlImgTxt.textProperty().addListener((obs, oldText, newText) -> CacheHandler.updateCache(oldText, newText, false));
            box.selectedProperty().addListener((obs, oldVa, newVal) -> CacheHandler.updateCache(oldVa.toString(), newVal.toString(), false));

            //DROPDOWN BUTTON
            drpDown.setValue("FIRST");
            drpDown.setOnAction(e -> {try {CacheHandler.setSettings(CacheHandler.getSelection());} catch (JSONException ignored) {}});

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
                    upd, shut, drpDown
            );

            //SYSTEM TRAY FUNCTION
            window.iconifiedProperty().addListener((prop, oldValue, newValue) -> {
                if (newValue) {
                    if (!SystemTray.isSupported()) {
                        logger.error("SystemTray is not supported");
                        Windows.display("An error has occured", "The OS you are using does not support System tray",true, true);
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
                        Windows.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true, true);
                        System.exit(0);
                    }
                    trayIcon.setToolTip(appName);
                    trayIcon.displayMessage("Info!", appName +" is now running in background",  TrayIcon.MessageType.INFO);
                }
            });

            Scene scene = new Scene(grid, 520, 700);
            window.getIcons().add(new javafx.scene.image.Image(icon));
            window.setScene(scene);
            window.setResizable(false);
            window.show();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Windows.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true, true);
            System.exit(0);
        }
    }

    private static Label getLabel(String text, int row) {
        Label label = new Label(text);
        GridPane.setConstraints(label, 0, row);
        return label;
    }

    private static TextField getTextField(int row) {
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
        CacheHandler.updateCache("", "", true);
        logger.info("Shutting down DiscordRPC library");
        thread.interrupt();
        logger.info("Closing application");
        System.exit(0);
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
            Windows.display("An error has occured", ex.getMessage()+"\n\nPlease report this to the github page.",true, true);
            System.exit(0);
        }
        return Toolkit.getDefaultToolkit().getImage(url);
    }

    private final String body = requestGET("https://api.github.com/repos/Bumbleboss/"+App.appName+"/releases/latest");
    private final JSONObject json = new JSONObject(Objects.requireNonNull(body));

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
            Windows.display("An error has occured",
                    "Something happened while trying to check for a new version, " +
                            "please restart application.\nIf the problem still arises, please report it to the github page.",true, true);
            System.exit(0);
            return null;
        }
    }
}

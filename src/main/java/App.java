import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
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
import org.apache.log4j.BasicConfigurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;


public class App extends Application {
    private final String appName = "custom-rpc-java";
    private final Logger logger = LoggerFactory.getLogger(appName);
    private File cache = new File("cache.json");
    private static final String icon = App.class.getResource("256x256.png").toExternalForm().replace("20%", " ");
    private static final String strayIcon = App.class.getResource("16x16.png").toExternalForm().replace("20%", " ");
    private Stage window;

    //TEXT FIELDS & CHECKBOX
    private TextField clientText = getTextField(1);
    private TextField detailsText = getTextField(3);
    private TextField stateText = getTextField(5);
    private TextField lrgImgKey = getTextField(9);
    private TextField smlImgKey = getTextField(11);
    private TextField lrgImgTxt = getTextField(13);
    private TextField smlImgTxt = getTextField(15);
    private CheckBox box = new CheckBox();

    public static void main(String[] args) {
        BasicConfigurator.configure();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String font = "TipoType_Brother_1816_Medium.otf";
        Font.loadFont(App.class.getResource(font).toExternalForm().replace("%20", " "), 36);
        Platform.setImplicitExit(false);
        try {
            window = primaryStage;
            window.setTitle(appName);

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
            box.setPadding(new Insets(0, 0, 0, 130));
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
                    getLabel("DETAILS*", 2), detailsText,
                    getLabel("STATE*", 4), stateText,
                    getLabel("ENABLE TIME", 6), box,
                    getLabel("LARGE IMAGE KEY", 8), lrgImgKey,
                    getLabel("SMALL IMAGE KEY", 10), smlImgKey,
                    getLabel("LARGE IMAGE TEXT", 12), lrgImgTxt,
                    getLabel("SMALL IMAGE TEXT", 14), smlImgTxt,
                    upd, shut, prev
            );

            window.iconifiedProperty().addListener((prop, oldValue, newValue) -> {
                if (newValue) {
                    if (!SystemTray.isSupported()) {
                        logger.error("SystemTray is not supported");
                        return;
                    }

                    window.hide();
                    final PopupMenu popup = new PopupMenu();
                    final TrayIcon trayIcon = new TrayIcon(getImage(strayIcon));
                    final SystemTray tray = SystemTray.getSystemTray();

                    MenuItem showApp = new MenuItem("Show application");
                    MenuItem exitBtn = new MenuItem("Exit");

                    exitBtn.addActionListener(es -> Platform.runLater(() -> {
                        closeApp(thread);
                    }));

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
                    }
                    trayIcon.setToolTip(appName);
                    trayIcon.displayMessage("Info!", appName +" is now running in background",  TrayIcon.MessageType.INFO);
                }
            });

            Scene scene = new Scene(grid, 620, 700);
            scene.getStylesheets().add("style.css");
            window.getIcons().add(new javafx.scene.image.Image(icon));
            window.setScene(scene);
            window.setResizable(false);
            window.show();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
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
            if(Confirm.display("Confirm","Do you want to save your input data?")) {
                try{
                    cache.createNewFile();
                    logger.info("Created cache file!");
                    writeCache();
                    logger.info("Written input data to the cahce file!");
                }catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
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
                    sb.append(line+"\n");
                }
                br.close();
            }
            fileReader.close();
            return sb.toString();
        }catch (Exception ex) {
            if(ex instanceof FileNotFoundException) {
                return "File was not found";
            }else{
                return ex.getMessage();
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

    private Image getImage(String directory) {
        URL url = null;
        try {
            url = new URL(directory);
        } catch (MalformedURLException ex) {
            logger.error("importing image encountered an error", ex);
        }
        return Toolkit.getDefaultToolkit().getImage(url);
    }
}

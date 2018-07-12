import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
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
import javafx.stage.StageStyle;


public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String font = "TipoType_Brother_1816_Medium.otf";
        Font.loadFont(App.class.getResource(font).toExternalForm().replace("%20", " "), 36);

        try {
            Stage window;
            window = primaryStage;
            window.setTitle("Custom RPC Java");

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(10, 10, 10, 10));
            grid.setVgap(8);
            grid.setHgap(10);

            //DISCORD RPC
            DiscordRPC lib = DiscordRPC.INSTANCE;
            DiscordRichPresence presence = new DiscordRichPresence();
            DiscordEventHandlers handlers = new DiscordEventHandlers();

            //CHECKBOX
            CheckBox box = new CheckBox();
            box.setPrefSize(17, 17);
            box.setPadding(new Insets(0, 0, 0, 130));
            GridPane.setConstraints(box, 0, 6);

            //TEXT FIELDS
            TextField clientText = getTextField(1);
            TextField detailsText = getTextField(3);
            TextField stateText = getTextField(5);
            TextField lrgImgKey = getTextField(9);
            TextField smlImgKey = getTextField(11);
            TextField lrgImgTxt = getTextField(13);
            TextField smlImgTxt = getTextField(15);

            //BUTTONS
            Button upd = getButton("UPDATE", 0);
            Button shut = getButton("SHUTDOWN", 187);
            Button prev = getButton("PREVIEW", 400);

            //ENABLES BUTTON AFTER USER INPUT
            BooleanBinding booleanBind = clientText.textProperty().isEmpty();
            upd.disableProperty().bind(booleanBind);

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
            window.setOnCloseRequest(e -> closeApp(lib));
            shut.setOnAction(e -> closeApp(lib));

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

            Scene scene = new Scene(grid, 620, 700);
            scene.getStylesheets().add("style.css");
            window.setScene(scene);
            window.setResizable(false);
            window.show();
        }catch (Exception ex) {
            ex.printStackTrace();
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

    private void closeApp(DiscordRPC lib) {
        lib.Discord_Shutdown();
        System.exit(0);
    }
}

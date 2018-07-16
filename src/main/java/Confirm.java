import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Confirm {

    static boolean answer;

    public static boolean display(String title, String message) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        Label label = new Label();
        label.setText(message);

        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");

        GridPane.setConstraints(label, 0, 0);
        GridPane.setConstraints(yesButton, 0, 1);
        GridPane.setConstraints(noButton, 0, 1);

        GridPane.setMargin(yesButton, new Insets(0,0,0, 90));
        GridPane.setMargin(noButton, new Insets(0,0,0, 160));

        yesButton.setOnAction(e -> {
            answer = true;
            window.close();
        });

        noButton.setOnAction(e -> {
            answer = false;
            window.close();
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);
        grid.getChildren().addAll(label, yesButton, noButton);
        grid.setAlignment(Pos.CENTER);

        Scene scene = new Scene(grid, 500, 100);
        scene.getStylesheets().add("style.css");
        window.setScene(scene);
        window.setResizable(false);
        window.showAndWait();
        return answer;
    }
}

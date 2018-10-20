import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

@SuppressWarnings("WeakerAccess")
public class Windows {

    private static boolean answer;

    public static boolean display(String title, String message, boolean alert, boolean wait) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);

        Label label = new Label();
        label.setText(message);
        label.setPadding(new Insets(15));

        GridPane grid = new GridPane();
        if(!alert) {
            Button yesButton = new Button("Yes");
            Button noButton = new Button("No");

            GridPane.setConstraints(label, 0, 0);
            GridPane.setConstraints(yesButton, 1, 1);
            GridPane.setConstraints(noButton, 2, 1);

            yesButton.setOnAction(e -> {
                answer = true;
                window.close();
            });

            noButton.setOnAction(e -> {
                answer = false;
                window.close();
            });

            grid.getChildren().addAll(label, yesButton, noButton);
        }else{
            Button okButton = new Button("OK");
            GridPane.setConstraints(label, 0, 0);
            GridPane.setConstraints(okButton, 1, 1);

            okButton.setOnAction(e -> {
                answer = true;
                window.close();
            });

            grid.getChildren().addAll(label, okButton);
        }

        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);
        grid.setAlignment(Pos.CENTER);

        Scene scene = new Scene(grid);
        window.setScene(scene);
        window.setResizable(false);

        if(!wait) {
            window.show();
        }else{
            window.showAndWait();
        }
        return answer;
    }
}

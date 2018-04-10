import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application
{
    Stage window;
    private Text textHolder = new Text();
    private double oldHeight = 0;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        window = primaryStage;
        window.setTitle("Minimal Auto Editor");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Edit your path here nerd");

        textArea.setPrefSize(200, 500);
        textArea.setWrapText(true);

        VBox vBox = new VBox();

        vBox.setMinWidth(500);
        vBox.setMinHeight(500);

        textHolder.textProperty().bind(textArea.textProperty());
        textHolder.layoutBoundsProperty().addListener(new ChangeListener<Bounds>()
        {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue)
            {
                if (oldHeight != newValue.getHeight())
                {
                    oldHeight = newValue.getHeight();
                    textArea.setPrefHeight(vBox.getHeight() + 20); // +20 is for paddings
                }
            }
        });


        vBox.getChildren().add(textArea);

        Scene scene = new Scene(vBox);
        window.setScene(scene);
        window.show();
    }
}

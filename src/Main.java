import com.jcraft.jsch.*;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application
{
    Stage window;
    private Text textHolder = new Text();
    private TextArea textArea = new TextArea();
    private VBox vBox = new VBox();
    private HBox hBox = new HBox();

    private NetworkFileManager fileManager = new NetworkFileManager();

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        window = primaryStage;
        window.setTitle("Minimal Auto Editor");

        textArea.setPromptText("Edit your path here nerd");

        textArea.setPrefSize(200, 500);
        textArea.setWrapText(true);

        vBox.setMinWidth(500);
        vBox.setMinHeight(500);

        hBox.setMinWidth(vBox.getMinWidth());

        Button readButton = new Button("Read");
        readButton.setOnAction(e -> readButtonClicked());

        Button writeButton = new Button("Write");
        writeButton.setOnAction(e -> writeButtonClicked());

        hBox.setPadding(new Insets(10));
        hBox.setSpacing(10);
        hBox.getChildren().addAll(readButton, writeButton);

        textHolder.textProperty().bind(textArea.textProperty());


        vBox.getChildren().addAll(textArea, hBox);

        Scene scene = new Scene(vBox);
        window.setScene(scene);

        window.heightProperty().addListener(this::updateHeight);

        window.show();
    }

    public void updateHeight(ObservableValue<? extends Number> observable, Number oldHeight, Number newHeight)
    {
        textArea.setPrefHeight(vBox.getHeight() + 20);
    }

    public void writeButtonClicked()
    {
        try (PrintWriter out = new PrintWriter("output.txt")) {
            out.println(textArea.getText());
        }
        catch(FileNotFoundException e)
        {
            System.out.println("File not found!");
        }

        writeFile();
    }

    public void readButtonClicked()
    {
        fileManager.recvFile("input.txt", "/U/testOutput.txt");

        try {
            byte[] encoded = Files.readAllBytes(Paths.get("input.txt"));
            textArea.setText(new String(encoded, "utf-8"));
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }

    public void writeFile()
    {
        fileManager.sendFile("output.txt", "/U/testOutput.txt");
    }
}

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application
{
    private Stage window;
    private Text textHolder = new Text();
    private TextArea textArea = new TextArea();
    private VBox vBox = new VBox();
    private HBox hBox = new HBox();

    private Text feedbackText = new Text();
    private TextField remoteFile = new TextField();

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

        remoteFile.setPromptText("Full remote file path");
        remoteFile.setMinWidth(100);

        Button readButton = new Button("Read");
        readButton.setOnAction(e -> readButtonClicked());

        Button writeButton = new Button("Write");
        writeButton.setOnAction(e -> writeButtonClicked());

        hBox.setPadding(new Insets(10));
        hBox.setSpacing(10);
        hBox.setMaxHeight(45);
        hBox.getChildren().addAll(remoteFile, readButton, writeButton, feedbackText);

        textHolder.textProperty().bind(textArea.textProperty());

        vBox.getChildren().addAll(textArea, hBox);

        Scene scene = new Scene(vBox);
        window.setScene(scene);

        window.heightProperty().addListener(this::updateHeight);

        window.show();

        Files.deleteIfExists(Paths.get("output.txt"));
        Files.deleteIfExists(Paths.get("input.txt"));
    }

    public void updateHeight(ObservableValue<? extends Number> observable, Number oldHeight, Number newHeight)
    {
        textArea.setPrefHeight(vBox.getHeight() + 20);
        hBox.setMaxHeight(45);
    }

    public void writeButtonClicked()
    {
        feedbackText.setText("");

        //Write the current text to a file
        try (PrintWriter out = new PrintWriter("output.txt")) {
            out.println(textArea.getText());
        }
        catch(FileNotFoundException e)
        {
            System.out.println("File not found!");
        }

        //Send the file
        String remote = remoteFile.getText();

        if(remote.equals(""))
        {
            feedbackText.setText("Please enter a remote file path");
            return;
        }

        String res = fileManager.sendFile("output.txt", remote);

        feedbackText.setText(res);
    }

    public void readButtonClicked()
    {
        feedbackText.setText("");

        String remote = remoteFile.getText();

        if(remote.equals(""))
        {
            feedbackText.setText("Please enter a remote file path");
            return;
        }

        //Get the file
        String res = fileManager.recvFile("input.txt", remote);


        feedbackText.setText(res);

        //Read the file to the window
        try
        {
            byte[] encoded = Files.readAllBytes(Paths.get("input.txt"));

            ObjectMapper mapper = new ObjectMapper();
            Object obj = mapper.readValue(new String(encoded, "utf-8"), Object.class);
            String out = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);

            textArea.setText(out);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }
}

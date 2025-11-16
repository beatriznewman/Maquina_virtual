package com.example.virtualmachine;

import com.example.virtualmachine.InterfaceVM;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class InterfaceVM extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(InterfaceVM.class.getResource("vm.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Máquina Virtual");
        stage.setScene(scene);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("Style.css")).toExternalForm());
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
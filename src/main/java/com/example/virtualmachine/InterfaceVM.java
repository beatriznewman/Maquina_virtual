package com.example.virtualmachine;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class InterfaceVM extends Application {
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            InterfaceVM.class.getResource("vm.fxml")
        );

        // *** CORREÇÃO: Tamanho adequado da janela ***
        Scene scene = new Scene(fxmlLoader.load(), 1200, 700);
        
        stage.setTitle("Máquina Virtual");
        stage.setScene(scene);
        
        // Carregar CSS
        String css = Objects.requireNonNull(
            getClass().getResource("Style.css")
        ).toExternalForm();
        scene.getStylesheets().add(css);
        
        // Configurações da janela
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
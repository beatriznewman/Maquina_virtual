package com.example.virtualmachine;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import virtualMachine.LinhaVM;
import virtualMachine.Memoria;
import virtualMachine.VirtualMachine;

import java.io.File;

public class InterfaceVMController {

    private File objFile;
    private VirtualMachine vm;

    @FXML private TableView<LinhaVM> tabela;
    @FXML private TableColumn<LinhaVM, Integer> linha;
    @FXML private TableColumn<LinhaVM, String> rotulo;
    @FXML private TableColumn<LinhaVM, String> instrucao;
    @FXML private TableColumn<LinhaVM, String> atributo1;
    @FXML private TableColumn<LinhaVM, String> atributo2;

    @FXML private TableView<Memoria> tabelaMemoria;
    @FXML private TableColumn<Memoria, Integer> valor;
    @FXML private TableColumn<Memoria, Integer> endereco;

    @FXML private TextArea saidaDeDados;

    @FXML
    protected void openFileVM() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Escolha um arquivo .obj");

        objFile = fileChooser.showOpenDialog(null);

        if (objFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Nenhum arquivo selecionado.");
            alert.show();
            return;
        }

        vm = new VirtualMachine(objFile.getAbsolutePath());

        // Configura colunas
        linha.setCellValueFactory(new PropertyValueFactory<>("linha"));
        rotulo.setCellValueFactory(new PropertyValueFactory<>("rotulo"));
        instrucao.setCellValueFactory(new PropertyValueFactory<>("instrucao"));
        atributo1.setCellValueFactory(new PropertyValueFactory<>("var1"));
        atributo2.setCellValueFactory(new PropertyValueFactory<>("var2"));

        // Carrega linhas do arquivo
        var linhas = vm.listaLinhas();
        tabela.setItems(FXCollections.observableArrayList(linhas));

        // limpa saída
        tabelaMemoria.setItems(FXCollections.observableArrayList());
        saidaDeDados.setText("");
    }

    @FXML
    protected void executar() {

        if (vm == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Nenhum arquivo carregado!");
            alert.show();
            return;
        }

        vm.analisaObj();

        endereco.setCellValueFactory(new PropertyValueFactory<>("endereco"));
        valor.setCellValueFactory(new PropertyValueFactory<>("valor"));

        tabelaMemoria.setItems(FXCollections.observableArrayList(vm.getMemoria()));

        saidaDeDados.setText(vm.getSaida());
    }
}

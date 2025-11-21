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

    // *** MELHORIA: Inicializar colunas uma vez só ***
    @FXML
    public void initialize() {
        // Configura colunas da tabela de código
        linha.setCellValueFactory(new PropertyValueFactory<>("linha"));
        rotulo.setCellValueFactory(new PropertyValueFactory<>("rotulo"));
        instrucao.setCellValueFactory(new PropertyValueFactory<>("instrucao"));
        atributo1.setCellValueFactory(new PropertyValueFactory<>("var1"));
        atributo2.setCellValueFactory(new PropertyValueFactory<>("var2"));

        // Configura colunas da tabela de memória
        endereco.setCellValueFactory(new PropertyValueFactory<>("endereco"));
        valor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        
        System.out.println("Controller inicializado!");
    }

    @FXML
    protected void openFileVM() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Escolha um arquivo .obj");
        
        // *** MELHORIA: Filtro de extensão ***
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Arquivos OBJ", "*.obj")
        );
        
        // *** MELHORIA: Diretório inicial (opcional) ***
        File dirInicial = new File(System.getProperty("user.dir"));
        if (dirInicial.exists()) {
            fileChooser.setInitialDirectory(dirInicial);
        }

        objFile = fileChooser.showOpenDialog(null);

        if (objFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Nenhum arquivo selecionado.");
            alert.show();
            return;
        }

        try {
            vm = new VirtualMachine(objFile.getAbsolutePath());

            // Carrega linhas do arquivo
            var linhas = vm.listaLinhas();
            tabela.setItems(FXCollections.observableArrayList(linhas));

            // Limpa memória e saída
            tabelaMemoria.setItems(FXCollections.observableArrayList());
            saidaDeDados.clear();
            
            // *** MELHORIA: Feedback visual ***
            saidaDeDados.appendText("✓ Arquivo carregado: " + objFile.getName() + "\n");
            saidaDeDados.appendText("  Total de linhas: " + linhas.size() + "\n");
            saidaDeDados.appendText("\nPronto para executar!\n");
            saidaDeDados.appendText("═".repeat(50) + "\n\n");
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao carregar arquivo");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
        }
    }

    @FXML
    protected void executar() {

        if (vm == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Nenhum arquivo carregado!");
            alert.show();
            return;
        }

        try {
            // Limpa saída anterior
            saidaDeDados.clear();
            saidaDeDados.appendText("═".repeat(50) + "\n");
            saidaDeDados.appendText("      EXECUTANDO PROGRAMA\n");
            saidaDeDados.appendText("═".repeat(50) + "\n\n");

            // Executa o programa
            vm.analisaObj();

            // Atualiza tabela de memória (apenas posições usadas)
            tabelaMemoria.setItems(FXCollections.observableArrayList(vm.getMemoria()));

            // Mostra saída do programa
            String saida = vm.getSaida();
            if (saida != null && !saida.trim().isEmpty()) {
                saidaDeDados.appendText("SAÍDA DO PROGRAMA:\n");
                saidaDeDados.appendText("─".repeat(50) + "\n");
                saidaDeDados.appendText(saida);
                
                // Remove linhas vazias extras no final
                if (!saida.endsWith("\n")) {
                    saidaDeDados.appendText("\n");
                }
            } else {
                saidaDeDados.appendText("(Programa não gerou saída)\n");
            }

            // Mensagem final
            saidaDeDados.appendText("\n" + "═".repeat(50) + "\n");
            saidaDeDados.appendText("✓ Execução concluída com sucesso!\n");
            saidaDeDados.appendText("  Memória utilizada: " + vm.getMemoria().size() + " posições\n");
            saidaDeDados.appendText("  Maior endereço: " + (vm.getMemoria().size() - 1) + "\n");
            saidaDeDados.appendText("═".repeat(50) + "\n");

            // *** MELHORIA: Rolar para o topo ***
            saidaDeDados.setScrollTop(0);

        } catch (Exception e) {
            // Mensagem de erro na área de saída
            saidaDeDados.appendText("\n\n");
            saidaDeDados.appendText("✗".repeat(50) + "\n");
            saidaDeDados.appendText("      ERRO DE EXECUÇÃO\n");
            saidaDeDados.appendText("✗".repeat(50) + "\n\n");
            saidaDeDados.appendText("Erro: " + e.getMessage() + "\n");
            
            // Alert de erro
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro de Execução");
            alert.setHeaderText("Ocorreu um erro durante a execução");
            alert.setContentText(e.getMessage());
            alert.show();
            
            e.printStackTrace();
        }
    }
}
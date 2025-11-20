package virtualMachine;

import java.io.File;
import java.io.FileWriter;

/**
 * Classe para testar a VM sem interface gráfica
 * Cria arquivos temporários e testa diretamente
 */
public class TestarSemGUI {
    
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("     TESTE DA MÁQUINA VIRTUAL (SEM INTERFACE)");
        System.out.println("═══════════════════════════════════════════════════════\n");
        
        // Teste 1: Operação simples
        System.out.println("Teste 1: Adição simples");
        testar("START\nLDC 5\nLDC 3\nADD\nPRN\nHLT\n", "8");
        
        // Teste 2: Carregar e armazenar
        System.out.println("\nTeste 2: Carregar e armazenar");
        testar("START\nLDC 42\nSTR 0\nLDV 0\nPRN\nHLT\n", "42");
        
        // Teste 3: Comparação
        System.out.println("\nTeste 3: Comparação (5 < 10)");
        testar("START\nLDC 5\nLDC 10\nCME\nPRN\nHLT\n", "1");
        
        // Teste 4: Multiplicação e subtração
        System.out.println("\nTeste 4: Multiplicação e subtração");
        testar("START\nLDC 10\nLDC 2\nMULT\nLDC 5\nSUB\nPRN\nHLT\n", "15");
        
        // Teste 5: Testar arquivo real se existir
        System.out.println("\nTeste 5: Arquivo gera4.obj");
        testarArquivo("gera4.obj");
        
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("✅ Testes concluídos!");
        System.out.println("═══════════════════════════════════════════════════════");
    }
    
    private static void testar(String codigo, String esperado) {
        try {
            File tempFile = File.createTempFile("test_vm_", ".obj");
            tempFile.deleteOnExit();
            
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(codigo);
            }
            
            VirtualMachine vm = new VirtualMachine(tempFile.getAbsolutePath());
            vm.analisaObj();
            
            String saida = vm.getSaida();
            if (saida != null && saida.trim().contains(esperado)) {
                System.out.println("   ✅ PASSOU - Saída: " + saida.trim());
            } else {
                System.out.println("   ❌ FALHOU - Esperado: " + esperado + ", Obtido: " + (saida != null ? saida.trim() : "null"));
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ ERRO: " + e.getMessage());
        }
    }
    
    private static void testarArquivo(String nomeArquivo) {
        try {
            File arquivo = new File(nomeArquivo);
            
            if (!arquivo.exists()) {
                arquivo = new File("../" + nomeArquivo);
                if (!arquivo.exists()) {
                    System.out.println("   ⚠️  Arquivo não encontrado: " + nomeArquivo);
                    return;
                }
            }
            
            VirtualMachine vm = new VirtualMachine(arquivo.getAbsolutePath());
            System.out.println("   📄 Arquivo: " + arquivo.getAbsolutePath());
            
            var linhas = vm.listaLinhas();
            System.out.println("   📋 Linhas carregadas: " + linhas.size());
            
            vm.analisaObj();
            
            String saida = vm.getSaida();
            if (saida != null && !saida.trim().isEmpty()) {
                System.out.println("   📤 Saída: " + saida.trim().replace("\n", ", "));
            } else {
                System.out.println("   ⚠️  Sem saída produzida");
            }
            
            System.out.println("   ✅ Execução concluída");
            
        } catch (Exception e) {
            System.out.println("   ❌ ERRO: " + e.getMessage());
            if (e.getMessage().contains("RD")) {
                System.out.println("   ⚠️  Nota: Este arquivo requer entrada do usuário (RD).");
                System.out.println("   ⚠️  Teste via interface gráfica para entrada interativa.");
            }
        }
    }
}


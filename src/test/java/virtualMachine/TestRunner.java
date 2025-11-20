package virtualMachine;

import java.io.File;
import java.util.Scanner;

/**
 * Classe para testar a VM sem interface gráfica
 * Executa todos os arquivos .obj disponíveis
 */
public class TestRunner {
    
    private static final boolean VERBOSE = true;
    
    public static void main(String[] args) {
        System.out.println("=== Teste da Máquina Virtual ===\n");
        
        // Lista de arquivos para testar
        String[] arquivos = {
            "gera.obj",
            "gera3.obj",
            "gera4.obj",
            "gera5.obj",
            "gera6.obj",
            "gera7.obj",
            "ger2.obj"
        };
        
        for (String arquivo : arquivos) {
            testarArquivo(arquivo);
        }
        
        System.out.println("\n=== Testes concluídos ===");
    }
    
    private static void testarArquivo(String nomeArquivo) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Testando: " + nomeArquivo);
        System.out.println("=".repeat(60));
        
        try {
            File arquivo = new File(nomeArquivo);
            
            if (!arquivo.exists()) {
                // Tenta no diretório pai
                arquivo = new File("../" + nomeArquivo);
                if (!arquivo.exists()) {
                    System.out.println("❌ Arquivo não encontrado: " + nomeArquivo);
                    return;
                }
            }
            
            System.out.println("Arquivo encontrado: " + arquivo.getAbsolutePath());
            
            VirtualMachine vm = new VirtualMachine(arquivo.getAbsolutePath());
            
            // Lista as linhas carregadas
            var linhas = vm.listaLinhas();
            System.out.println("\nLinhas carregadas: " + linhas.size());
            
            if (VERBOSE) {
                System.out.println("\nCódigo carregado:");
                for (int i = 0; i < linhas.size(); i++) {
                    var l = linhas.get(i);
                    System.out.printf("  [%2d] %-8s %-8s %-8s %-8s\n", 
                        i, 
                        l.getRotulo().isEmpty() ? "-" : l.getRotulo(),
                        l.getInstrucao().isEmpty() ? "-" : l.getInstrucao(),
                        l.getVar1().isEmpty() ? "-" : l.getVar1(),
                        l.getVar2().isEmpty() ? "-" : l.getVar2()
                    );
                }
            }
            
            System.out.println("\n--- Executando VM ---");
            
            // Executa a VM
            vm.analisaObj();
            
            // Mostra a saída
            String saida = vm.getSaida();
            System.out.println("\n--- SAÍDA ---");
            if (saida != null && !saida.trim().isEmpty()) {
                System.out.print(saida);
            } else {
                System.out.println("(nenhuma saída)");
            }
            
            // Mostra estado da memória (apenas primeiros 20 endereços com valor != 0)
            var memoria = vm.getMemoria();
            if (memoria != null && !memoria.isEmpty()) {
                System.out.println("\n--- MEMÓRIA (endereços não-zero) ---");
                int count = 0;
                for (Memoria m : memoria) {
                    if (m.getValor() != 0 && count < 20) {
                        System.out.printf("  [%3d] = %d\n", m.getEndereco(), m.getValor());
                        count++;
                    }
                }
                if (count == 0) {
                    System.out.println("  (todos os valores são zero)");
                }
            }
            
            System.out.println("\n✅ Teste concluído com sucesso!");
            
        } catch (Exception e) {
            System.out.println("\n❌ ERRO ao testar " + nomeArquivo + ":");
            System.out.println("   " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
        }
    }
}


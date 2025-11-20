package virtualMachine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe para testar arquivos .obj da VM
 * Testa todos os arquivos disponíveis sem interface gráfica
 */
public class TestarArquivos {
    
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("     TESTE DA MÁQUINA VIRTUAL - TODOS OS ARQUIVOS");
        System.out.println("═══════════════════════════════════════════════════════\n");
        
        // Lista de arquivos para testar
        String[] arquivos = {
            "gera4.obj",   // Teste simples de adição
            "gera3.obj",   // Teste de if-else
            "ger2.obj",    // Teste de if-else com entrada
            "gera5.obj",   // Teste com CALL
            "gera6.obj",   // Teste com ALLOC/DALLOC
            "gera7.obj",   // Teste com CALL e ALLOC/DALLOC
            "gera.obj"     // Teste complexo completo
        };
        
        int sucessos = 0;
        int falhas = 0;
        
        for (String nomeArquivo : arquivos) {
            System.out.println("\n" + "─".repeat(60));
            System.out.println("📄 Testando: " + nomeArquivo);
            System.out.println("─".repeat(60));
            
            try {
                File arquivo = new File(nomeArquivo);
                
                if (!arquivo.exists()) {
                    // Tenta encontrar no diretório do projeto
                    arquivo = new File("../" + nomeArquivo);
                    if (!arquivo.exists()) {
                        System.out.println("❌ Arquivo não encontrado: " + nomeArquivo);
                        falhas++;
                        continue;
                    }
                }
                
                System.out.println("✓ Arquivo encontrado: " + arquivo.getAbsolutePath());
                
                VirtualMachine vm = new VirtualMachine(arquivo.getAbsolutePath());
                
                // Carrega e exibe código
                var linhas = vm.listaLinhas();
                System.out.println("\n📋 Código carregado (" + linhas.size() + " linhas):");
                System.out.println("   Linha | Rótulo | Instrução | Var1 | Var2");
                System.out.println("   " + "─".repeat(50));
                
                for (int i = 0; i < linhas.size() && i < 10; i++) {
                    var l = linhas.get(i);
                    System.out.printf("   %4d  | %-6s | %-9s | %-4s | %-4s\n", 
                        i,
                        l.getRotulo().isEmpty() ? "-" : l.getRotulo(),
                        l.getInstrucao().isEmpty() ? "-" : l.getInstrucao(),
                        l.getVar1().isEmpty() ? "-" : l.getVar1(),
                        l.getVar2().isEmpty() ? "-" : l.getVar2()
                    );
                }
                
                if (linhas.size() > 10) {
                    System.out.println("   ... (" + (linhas.size() - 10) + " linhas restantes)");
                }
                
                System.out.println("\n▶ Executando VM...\n");
                
                // Executa a VM
                vm.analisaObj();
                
                // Mostra saída
                String saida = vm.getSaida();
                System.out.println("\n📤 SAÍDA:");
                if (saida != null && !saida.trim().isEmpty()) {
                    String[] linhasSaida = saida.split("\n");
                    for (String linha : linhasSaida) {
                        if (!linha.trim().isEmpty()) {
                            System.out.println("   → " + linha.trim());
                        }
                    }
                } else {
                    System.out.println("   (nenhuma saída produzida)");
                }
                
                System.out.println("\n✅ Teste concluído com sucesso!");
                sucessos++;
                
            } catch (Exception e) {
                System.out.println("\n❌ ERRO ao executar " + nomeArquivo + ":");
                System.out.println("   " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (e.getCause() != null) {
                    System.out.println("   Causa: " + e.getCause().getMessage());
                }
                falhas++;
            }
        }
        
        // Resumo final
        System.out.println("\n" + "═".repeat(60));
        System.out.println("📊 RESUMO DOS TESTES");
        System.out.println("═".repeat(60));
        System.out.println("   ✅ Sucessos: " + sucessos);
        System.out.println("   ❌ Falhas: " + falhas);
        System.out.println("   📁 Total: " + (sucessos + falhas));
        
        if (falhas == 0) {
            System.out.println("\n🎉 Todos os testes passaram!");
        } else {
            System.out.println("\n⚠️  Alguns testes falharam. Verifique os erros acima.");
        }
    }
}


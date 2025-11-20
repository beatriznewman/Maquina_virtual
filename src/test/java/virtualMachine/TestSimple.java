package virtualMachine;

/**
 * Classe simples para testar rapidamente a VM
 * Simula entrada do usuário para testes com RD
 */
public class TestSimple {
    
    public static void main(String[] args) {
        System.out.println("=== Teste Simples da VM ===\n");
        
        // Teste 1: Operação simples
        System.out.println("Teste 1: Adição simples (5 + 3)");
        testarCodigo(
            "START\n" +
            "LDC 5\n" +
            "LDC 3\n" +
            "ADD\n" +
            "PRN\n" +
            "HLT\n"
        );
        
        // Teste 2: Carregar e armazenar
        System.out.println("\nTeste 2: Carregar constante e armazenar");
        testarCodigo(
            "START\n" +
            "LDC 42\n" +
            "STR 0\n" +
            "LDV 0\n" +
            "PRN\n" +
            "HLT\n"
        );
        
        // Teste 3: Comparação
        System.out.println("\nTeste 3: Comparação (5 < 10)");
        testarCodigo(
            "START\n" +
            "LDC 5\n" +
            "LDC 10\n" +
            "CME\n" +
            "PRN\n" +
            "HLT\n"
        );
    }
    
    private static void testarCodigo(String codigo) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("test_vm_", ".obj");
            tempFile.deleteOnExit();
            
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                writer.write(codigo);
            }
            
            VirtualMachine vm = new VirtualMachine(tempFile.getAbsolutePath());
            vm.analisaObj();
            
            String saida = vm.getSaida();
            if (saida != null && !saida.trim().isEmpty()) {
                System.out.println("Saída: " + saida.trim());
            } else {
                System.out.println("Sem saída");
            }
            
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


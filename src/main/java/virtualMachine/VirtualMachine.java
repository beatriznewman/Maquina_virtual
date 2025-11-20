package virtualMachine;

import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.util.*;

public class VirtualMachine {

    private final String path;
    private List<Memoria> memoria;
    private int s; // ponteiro da pilha (topo)
    private String saida;
    private final int STACK_OFFSET = 100;

    private final boolean DEBUG = false; // Altere para true para ver logs detalhados
    private final long STEP_LIMIT = 10_000L;

    private static final Set<String> OPCODES = Set.of(
            "START","LDC","LDV","STR","ADD","SUB","MULT","DIVI",
            "INV","NEG","AND","OR","CME","CMA","CEQ","CDIF","CMEQ","CMAQ",
            "RD","PRN","JMP","JMPF","ALLOC","DALLOC","CALL","RETURN","HLT"
    );

    public VirtualMachine(String path) {
        this.path = path;
    }

    private int achaLinha(List<LinhaVM> linhas, String rotulo) {
        if (rotulo == null) return -1;
        rotulo = rotulo.trim();
        for (int i = 0; i < linhas.size(); i++) {
            String r = linhas.get(i).getRotulo();
            if (r != null && r.trim().equals(rotulo)) return i;
        }
        return -1;
    }

    public List<LinhaVM> listaLinhas() {
        List<LinhaVM> linhas = new ArrayList<>();
        try {
            File file = new File(path);
            Scanner scanner = new Scanner(file);
            try {
            int numeroLinha = 1;
            while (scanner.hasNextLine()) {
                String raw = scanner.nextLine();
                if (raw == null) continue;
                String line = raw.trim();
                if (line.isEmpty()) continue;

                LinhaVM l = new LinhaVM();
                l.setLinha(numeroLinha++);

                String rotulo = "";
                String instr = "";
                String var1 = "";
                String var2 = "";

                if (raw.contains("\t")) {
                    String[] parts = raw.split("\t", 2);
                    rotulo = parts[0].trim();
                    if (parts.length == 1 || parts[1].trim().equalsIgnoreCase("NULL")) {
                        instr = "";
                    } else {
                        String resto = parts[1].trim();
                        String[] toks = resto.split("\\s+");
                        if (toks.length >= 1) instr = toks[0].trim().toUpperCase();
                        if (toks.length >= 2) var1 = toks[1].trim().replaceAll(",", "");
                        if (toks.length >= 3) var2 = toks[2].trim().replaceAll(",", "");
                    }
                } else {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 0) continue;
                    String first = parts[0].trim().toUpperCase();
                    if (OPCODES.contains(first)) {
                        instr = first;
                        if (parts.length >= 2) var1 = parts[1].trim().replaceAll(",", "");
                        if (parts.length >= 3) var2 = parts[2].trim().replaceAll(",", "");
                    } else {
                        rotulo = parts[0].trim();
                        if (parts.length == 1) {
                            instr = "";
                        } else {
                            String second = parts[1].trim().toUpperCase();
                            if (second.equalsIgnoreCase("NULL")) {
                                instr = "";
                            } else if (OPCODES.contains(second)) {
                                instr = second;
                                if (parts.length >= 3) var1 = parts[2].trim().replaceAll(",", "");
                                if (parts.length >= 4) var2 = parts[3].trim().replaceAll(",", "");
                            } else {
                                instr = second;
                                if (parts.length >= 3) var1 = parts[2].trim().replaceAll(",", "");
                                if (parts.length >= 4) var2 = parts[3].trim().replaceAll(",", "");
                            }
                        }
                    }
                }

                l.setRotulo(rotulo);
                l.setInstrucao(instr == null ? "" : instr);
                l.setVar1(var1 == null ? "" : var1);
                l.setVar2(var2 == null ? "" : var2);
                linhas.add(l);
            }
            return linhas;
            } finally {
                scanner.close();
            }
        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void analisaObj() {
        try {
            saida = "";
            List<LinhaVM> linhas = listaLinhas();

            // Monta tabela de rótulos
            Map<String, Integer> tabelaRotulos = new HashMap<>();
            for (int k = 0; k < linhas.size(); k++) {
                String r = linhas.get(k).getRotulo();
                if (r != null && !r.trim().isEmpty()) {
                    tabelaRotulos.put(r.trim(), k);
                }
            }

            memoria = new ArrayList<>();
            s = STACK_OFFSET - 1;
            ensureMemorySize(STACK_OFFSET + 100);

            long steps = 0;
            boolean executando = true;

            for (int i = 0; i < linhas.size() && executando; i++) {
                if (++steps > STEP_LIMIT) {
                    System.out.println("Limite de passos excedido (" + STEP_LIMIT + "). Possível loop infinito.");
                    break;
                }

                LinhaVM l = linhas.get(i);
                String op = l.getInstrucao();
                if (op == null) op = "";
                op = op.trim().toUpperCase();

                if (DEBUG) {
                    System.out.printf("[DEBUG] Linha %d: %s %s %s (s=%d)", i, op, l.getVar1(), l.getVar2(), s);
                    if (s >= STACK_OFFSET) {
                        System.out.printf(" topo=%d", memoria.get(s).getValor());
                    }
                    System.out.println();
                }

                if (op.isEmpty()) continue;

                String a = (l.getVar1() == null) ? "" : l.getVar1().trim();
                String b = (l.getVar2() == null) ? "" : l.getVar2().trim();

                switch (op) {
                    case "START":
                        // Limpa apenas a pilha, mantém dados
                        s = STACK_OFFSET - 1;
                        break;

                    case "LDC":
                        if (!isInteger(a)) break;
                        push(Integer.parseInt(a));
                        break;

                    case "LDV":
                        if (!isInteger(a)) break;
                        int addrLDV = Integer.parseInt(a);
                        ensureMemorySize(addrLDV);
                        push(memoria.get(addrLDV).getValor());
                        break;

                    case "STR":
                        if (!isInteger(a)) break;
                        if (!checkStackAtLeast(1, "STR")) break;
                        int valSTR = pop();
                        int addrSTR = Integer.parseInt(a);
                        ensureMemorySize(addrSTR);
                        memoria.set(addrSTR, new Memoria(addrSTR, valSTR));
                        break;

                    case "ADD": binOp((x,y)->x+y); break;
                    case "SUB": binOp((x,y)->x-y); break;
                    case "MULT": binOp((x,y)->x*y); break;
                    case "DIVI": binOp((x,y)-> y==0 ? 0 : x/y); break;
                    case "AND": binOp((x,y)->(x==1&&y==1)?1:0); break;
                    case "OR": binOp((x,y)->(x==1||y==1)?1:0); break;
                    case "CME": binOp((x,y)-> x<y?1:0); break;
                    case "CMA": binOp((x,y)-> x>y?1:0); break;
                    case "CEQ": binOp((x,y)-> x==y?1:0); break;
                    case "CDIF": binOp((x,y)-> x!=y?1:0); break;
                    case "CMEQ": binOp((x,y)-> x<=y?1:0); break;
                    case "CMAQ": binOp((x,y)-> x>=y?1:0); break;

                    case "INV":
                        if (!checkStackAtLeast(1,"INV")) break;
                        int valINV = pop();
                        push(-valINV);
                        break;

                    case "NEG":
                        if (!checkStackAtLeast(1,"NEG")) break;
                        int valNEG = pop();
                        push(valNEG == 0 ? 1 : 0); // Negação lógica
                        break;

                    case "RD": {
                        TextInputDialog dialog = new TextInputDialog("");
                        dialog.setTitle("Entrada");
                        dialog.setHeaderText("Digite um inteiro:");
                        Optional<String> user = dialog.showAndWait();
                        if (user.isEmpty()) {
                            throw new RuntimeException("Entrada cancelada pelo usuário (RD).");
                        }
                        try {
                            int val = Integer.parseInt(user.get().trim());
                            push(val);
                        } catch (NumberFormatException ex) {
                            push(0);
                        }
                        break;
                    }

                    case "PRN":
                        if (!checkStackAtLeast(1,"PRN")) break;
                        int valor = pop();
                        saida += valor + "\n";
                        System.out.println("SAÍDA: " + valor);
                        break;

                    case "JMP": {
                        // Primeiro tenta encontrar na tabela de rótulos
                        int destino = tabelaRotulos.getOrDefault(a, -1);
                        // Se não encontrou, tenta procurar manualmente (pode ser rótulo numérico)
                        if (destino < 0) destino = achaLinha(linhas, a);
                        // Se ainda não encontrou e é um número, tenta usar como índice direto
                        if (destino < 0 && isInteger(a)) {
                            destino = Integer.parseInt(a);
                            if (destino < 0 || destino >= linhas.size()) {
                                destino = -1;
                            }
                        }
                        if (destino < 0) {
                            System.out.println("[JMP] rótulo não encontrado: " + a);
                        } else {
                            i = destino - 1; // -1 porque o loop incrementa
                        }
                        break;
                    }

                    case "JMPF": {
                        if (!checkStackAtLeast(1,"JMPF")) break;
                        int val_jmpf = pop();
                        if (val_jmpf == 0) {
                            // Primeiro tenta encontrar na tabela de rótulos
                            int destino = tabelaRotulos.getOrDefault(a, -1);
                            // Se não encontrou, tenta procurar manualmente
                            if (destino < 0) destino = achaLinha(linhas, a);
                            // Se ainda não encontrou e é um número, tenta usar como índice direto
                            if (destino < 0 && isInteger(a)) {
                                destino = Integer.parseInt(a);
                                if (destino < 0 || destino >= linhas.size()) {
                                    destino = -1;
                                }
                            }
                            if (destino < 0) {
                                System.out.println("[JMPF] rótulo não encontrado: " + a);
                            } else {
                                i = destino - 1; // -1 porque o loop incrementa
                            }
                        }
                        break;
                    }

                    case "ALLOC": {
                        if (!isInteger(a) || !isInteger(b)) break;
                        int addr = Integer.parseInt(a);
                        int qtd = Integer.parseInt(b);
                        if (qtd < 0) break;
                        ensureMemorySize(addr + qtd);
                        
                        // CORREÇÃO CRÍTICA: Salva valores atuais na PILHA
                        for (int j = 0; j < qtd; j++) {
                            push(memoria.get(addr + j).getValor());
                        }
                        // Inicializa com zeros
                        for (int j = 0; j < qtd; j++) {
                            memoria.set(addr + j, new Memoria(addr + j, 0));
                        }
                        break;
                    }

                    case "DALLOC": {
                        if (!isInteger(a) || !isInteger(b)) break;
                        int addr = Integer.parseInt(a);
                        int qtd = Integer.parseInt(b);
                        if (qtd < 0) break;
                        
                        // CORREÇÃO CRÍTICA: Restaura valores da PILHA (ordem inversa)
                        for (int j = qtd - 1; j >= 0; j--) {
                            if (!checkStackAtLeast(1, "DALLOC")) break;
                            int valorRestaurado = pop();
                            ensureMemorySize(addr + j);
                            memoria.set(addr + j, new Memoria(addr + j, valorRestaurado));
                        }
                        break;
                    }

                    case "CALL": {
                        // Empilha endereço de retorno (linha atual + 1)
                        push(i + 1);
                        // Primeiro tenta encontrar na tabela de rótulos
                        int destino = tabelaRotulos.getOrDefault(a, -1);
                        // Se não encontrou, tenta procurar manualmente
                        if (destino < 0) destino = achaLinha(linhas, a);
                        // Se ainda não encontrou e é um número, tenta usar como índice direto
                        if (destino < 0 && isInteger(a)) {
                            destino = Integer.parseInt(a);
                            if (destino < 0 || destino >= linhas.size()) {
                                destino = -1;
                            }
                        }
                        if (destino < 0) {
                            System.out.println("[CALL] rótulo não encontrado: " + a);
                            pop(); // Remove endereço inválido
                        } else {
                            i = destino - 1; // -1 porque o loop incrementa
                        }
                        break;
                    }

                    case "RETURN":
                        if (!checkStackAtLeast(1, "RETURN")) {
                            System.out.println("ERRO CRÍTICO: Pilha vazia no RETURN");
                            executando = false;
                            break;
                        }
                        int ret = pop();
                        
                        // PROTEÇÃO CONTRA LOOP INFINITO
                        if (ret == i + 1) { // Se retornar para a mesma linha
                            System.out.println("ERRO: Loop infinito detectado no RETURN");
                            executando = false;
                            break;
                        }
                        
                        if (ret < 0 || ret >= linhas.size()) {
                            System.out.println("ERRO: Endereço de retorno inválido: " + ret);
                            executando = false;
                            break;
                        }
                        
                        i = ret - 1; // -1 porque o loop incrementa
                        if (DEBUG) {
                            System.out.println("[RETURN] Retornando para linha " + ret);
                        }
                        break;
                    case "HLT":
                        executando = false;
                        System.out.println("HLT executado. Steps: " + steps);
                        break;

                    default:
                        System.out.println("Instrução inválida: " + op);
                }
            }

            System.out.println("Execução finalizada. passos=" + steps);
            System.out.println("Saída final:\n" + saida);

        } catch (Exception e) {
            System.out.println("Erro execução: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureMemorySize(int index) {
        if (index < 0) return;
        while (memoria.size() <= index) {
            int addr = memoria.size();
            memoria.add(new Memoria(addr, 0));
        }
    }

    private void push(int v) {
        s++;
        ensureMemorySize(s);
        memoria.set(s, new Memoria(s, v));
    }

    private int pop() {
        if (s < STACK_OFFSET) {
            System.out.println("Pop em pilha vazia!");
            return 0;
        }
        int v = memoria.get(s).getValor();
        s--;
        return v;
    }

    private int top() {
        if (s < STACK_OFFSET) {
            System.out.println("Top em pilha vazia!");
            return 0;
        }
        return memoria.get(s).getValor();
    }

    private void binOp(Bin op) {
        if (!checkStackAtLeast(2, "binOp")) return;
        int y = pop();
        int x = pop();
        push(op.apply(x, y));
    }

    private boolean isInteger(String s) {
        if (s == null) return false;
        s = s.trim();
        if (s.isEmpty()) return false;
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    private boolean checkStackAtLeast(int need, String instr) {
        int have = s - STACK_OFFSET + 1;
        if (have < need) {
            System.out.println("Pilha insuficiente para " + instr + " (precisa " + need + ", tem " + have + ")");
            return false;
        }
        return true;
    }

    private interface Bin { int apply(int x, int y); }

    public List<Memoria> getMemoria() { return memoria; }
    public String getSaida() { return saida; }
}
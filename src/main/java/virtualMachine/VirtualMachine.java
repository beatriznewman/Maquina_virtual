package virtualMachine;

import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.util.*;

public class VirtualMachine {

    private final String path;

    // memória principal (cada posição é um objeto Memoria)
    private List<Memoria> memoria;

    // pilha (ponteiro s será um índice em 'memoria')
    private int s;

    private String saida;

    private final boolean DEBUG = false;
    private final long STEP_LIMIT = 1_000_000L;

    private static final Set<String> OPCODES = Set.of(
            "START","LDC","LDV","STR","ADD","SUB","MULT","DIVI",
            "INV","NEG","AND","OR","CME","CMA","CEQ","CDIF","CMEQ","CMAQ",
            "RD","PRN","JMP","JMPF","ALLOC","DALLOC","CALL","RETURN","HLT","NULL"
    );

    public VirtualMachine(String path) {
        this.path = path;
    }

    // representa uma instrução compactada (sem linhas "NULL" apenas instruções)
    private static class Instr {
        String op;
        String a;
        String b;
        public Instr(String op, String a, String b) { this.op = op; this.a = a; this.b = b; }
    }

    // lê o arquivo e retorna lista de LinhaVM (sem alterações)
    public List<LinhaVM> listaLinhas() {
        List<LinhaVM> linhas = new ArrayList<>();
        try {
            File file = new File(path);
            Scanner scanner = new Scanner(file);
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
            scanner.close();
            return linhas;
        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // principal: analisa o .obj e executa
    public void analisaObj() {
        try {
            saida = "";
            List<LinhaVM> linhas = listaLinhas();

            // Construir program (P) compactado e tabela de rótulos
            List<Instr> program = new ArrayList<>();
            Map<String, Integer> labels = new HashMap<>();

            for (int idx = 0; idx < linhas.size(); idx++) {
                LinhaVM l = linhas.get(idx);
                String rot = (l.getRotulo() == null ? "" : l.getRotulo().trim());
                String op = (l.getInstrucao() == null ? "" : l.getInstrucao().trim().toUpperCase());

                if (!op.isEmpty()) {
                    if (!rot.isEmpty()) {
                        labels.put(rot, program.size());
                    }
                    program.add(new Instr(op, l.getVar1(), l.getVar2()));
                } else {
                    if (!rot.isEmpty()) {
                        labels.put(rot, program.size());
                    }
                }
            }

            // *** CORREÇÃO 1: Inicializar memória com tamanho inicial pequeno ***
            memoria = new ArrayList<>();
            s = -1;
            // Não pré-alocar 2000 posições - crescer sob demanda

            long steps = 0;
            boolean executando = true;

            int i = 0;
            while (i < program.size() && executando) {
                if (++steps > STEP_LIMIT) {
                    System.out.println("Limite de passos excedido (" + STEP_LIMIT + "). Possivel loop infinito.");
                    break;
                }

                Instr inst = program.get(i);
                String op = (inst.op == null ? "" : inst.op.trim().toUpperCase());
                String a = (inst.a == null ? "" : inst.a.trim());
                String b = (inst.b == null ? "" : inst.b.trim());

                if (DEBUG) {
                    System.out.printf("[DEBUG] i=%d op=%s a=%s b=%s s=%d\n", i, op, a, b, s);
                }

                int i_anterior = i;
                switch (op) {
                    case "START":
                        s = -1;
                        break;

                    case "HLT":
                        executando = false;
                        this.guiPrintln("\nExecucao terminada (HLT).");
                        break;

                    case "ALLOC": {
                        int m = paramVal(a);
                        int n = paramVal(b);
                        
                        // ALLOC salva valores existentes de M[m..m+n-1] na pilha
                        for (int k = 0; k < n; k++) {
                            this.s += 1;
                            ensureMemorySize(this.s);  // Só garante espaço no topo da pilha
                            
                            // Se M[m+k] já existe, copia; senão usa 0 (memória não inicializada)
                            int valorAntigo = 0;
                            if (m + k < memoria.size()) {
                                valorAntigo = memoria.get(m + k).getValor();
                            }
                            memoria.get(this.s).setValor(valorAntigo);
                        }
                        break;
                    }

                    case "DALLOC": {
                        int m = paramVal(a);
                        int n = paramVal(b);
                        
                        // DALLOC restaura valores da pilha para M[m..m+n-1]
                        for (int k = n - 1; k >= 0; k--) {
                            if (this.s < 0) throw new RuntimeException("Stack underflow em DALLOC");
                            
                            // Garante que M[m+k] existe antes de escrever
                            ensureMemorySize(m + k);
                            
                            int valorPilha = memoria.get(this.s).getValor();
                            memoria.get(m + k).setValor(valorPilha);
                            this.s -= 1;
                        }
                        break;
                    }

                    case "LDC":
                        push(paramVal(a));
                        break;

                    case "LDV": {
                        int addr = paramVal(a);
                        ensureMemorySize(addr);
                        push(memoria.get(addr).getValor());
                        break;
                    }

                    case "STR": {
                        int addr = paramVal(a);
                        int val = pop();
                        ensureMemorySize(addr);
                        memoria.get(addr).setValor(val);
                        break;
                    }

                    case "ADD": binOp((x,y)->x+y); break;
                    case "SUB": binOp((x,y)->x-y); break;
                    case "MULT": binOp((x,y)->x*y); break;
                    case "DIVI": binOp((x,y)-> y==0 ? 0 : x/y); break;
                    case "INV": {
                        int v = pop();
                        push(-v);
                        break;
                    }
                    case "AND": binOp((x,y)->(x==1 && y==1) ? 1 : 0); break;
                    case "OR": binOp((x,y)->(x==1 || y==1) ? 1 : 0); break;
                    case "NEG": {
                        int v = pop();
                        push(1 - v);
                        break;
                    }
                    case "CME": binOp((x,y)-> x<y?1:0); break;
                    case "CMA": binOp((x,y)-> x>y?1:0); break;
                    case "CEQ": binOp((x,y)-> x==y?1:0); break;
                    case "CDIF": binOp((x,y)-> x!=y?1:0); break;
                    case "CMEQ": binOp((x,y)-> x<=y?1:0); break;
                    case "CMAQ": binOp((x,y)-> x>=y?1:0); break;

                    case "JMP": {
                        Integer destino = labels.getOrDefault(a, null);
                        if (destino == null) {
                            System.out.println("[JMP] rotulo nao encontrado: " + a);
                        } else {
                            i = destino;
                        }
                        break;
                    }

                    case "JMPF": {
                        int cond = pop();
                        if (cond == 0) {
                            Integer destino = labels.getOrDefault(a, null);
                            if (destino == null) {
                                System.out.println("[JMPF] rotulo nao encontrado: " + a);
                            } else {
                                i = destino;
                            }
                        }
                        break;
                    }

                    case "CALL": {
                        int target = labels.getOrDefault(a, -1);
                        int ret = i + 1;
                        push(ret);
                        if (target < 0) {
                            System.out.println("[CALL] rotulo nao encontrado: " + a);
                            pop();
                        } else {
                            i = target;
                        }
                        break;
                    }

                    case "RETURN": {
                        if (this.s < 0) throw new RuntimeException("RETURN com pilha vazia.");
                        int ret = pop();
                        i = ret;
                        break;
                    }

                    case "RD": {
                        TextInputDialog dialog = new TextInputDialog("");
                        dialog.setTitle("Entrada");
                        dialog.setHeaderText("Digite um inteiro:");
                        Optional<String> user = dialog.showAndWait();
                        if (user.isEmpty()) {
                            throw new RuntimeException("Entrada cancelada pelo usuario (RD).");
                        }
                        try {
                            int val = Integer.parseInt(user.get().trim());
                            push(val);
                        } catch (NumberFormatException ex) {
                            push(0);
                        }
                        break;
                    }

                    case "PRN": {
                        int v = pop();
                        guiPrintln(v);
                        break;
                    }

                    case "NULL":
                        break;

                    default:
                        throw new RuntimeException("Instrucao invalida: " + op);
                }

                if (i == i_anterior) {
                    i = i + 1;
                }
            }

            System.out.println("Execucao finalizada. passos=" + steps);
            System.out.println("Saida final:\n" + saida);

        } catch (Exception e) {
            System.out.println("Erro execucao: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // *** CORREÇÃO 4: ensureMemorySize agora só cresce até o necessário ***
    private void ensureMemorySize(int index) {
        if (index < 0) return;
        while (memoria.size() <= index) {
            int addr = memoria.size();
            memoria.add(new Memoria(addr, 0)); // Inicializa com 0
        }
    }

    private void push(int v) {
        s += 1;
        ensureMemorySize(s);
        memoria.get(s).setValor(v);
    }

    private int pop() {
        if (s < 0) {
            throw new RuntimeException("Pop em pilha vazia!");
        }
        int v = memoria.get(s).getValor();
        s -= 1;
        return v;
    }

    private void binOp(Bin op) {
        if (s < 1) throw new RuntimeException("Stack underflow em binOp");
        int y = pop();
        int x = pop();
        push(op.apply(x, y));
    }

    private int paramVal(String p) {
        if (p == null || p.trim().isEmpty()) return 0;
        try { return Integer.parseInt(p.trim()); } catch (Exception e) { return 0; }
    }

    private interface Bin { int apply(int x, int y); }

    private void guiPrintln(Object o) {
        this.saida += String.valueOf(o) + "\n";
        System.out.println(o);
    }

    public List<Memoria> getMemoria() { return memoria; }
    public String getSaida() { return saida; }
}
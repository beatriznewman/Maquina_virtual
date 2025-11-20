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

    private final int STACK_OFFSET = 100;
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

    // principal: analisa o .obj e executa, seguindo exatamente a semântica do Python
    public void analisaObj() {
        try {
            saida = "";
            List<LinhaVM> linhas = listaLinhas();

            // Construir program (P) compactado e tabela de rótulos compatível com Python
            List<Instr> program = new ArrayList<>();
            Map<String, Integer> labels = new HashMap<>(); // rotulo -> indice em program

            // Primeiro pass: quando encontramos uma linha com rótulo, registramos
            // mas em Python o label aponta para o índice do comando recém-adicionado.
            // Para simular, percorremos e, para cada linha:
            for (int idx = 0; idx < linhas.size(); idx++) {
                LinhaVM l = linhas.get(idx);
                String rot = (l.getRotulo() == null ? "" : l.getRotulo().trim());
                String op = (l.getInstrucao() == null ? "" : l.getInstrucao().trim().toUpperCase());

                // Se linha contiver uma instrução válida (não vazia), adicionamos e
                // depois, se havia rótulo, o rótulo aponta para índice do comando recém-adicionado.
                if (!op.isEmpty()) {
                    // se havia rótulo antes da instrução (linha pode ter rotulo + instr),
                    // então atribuimos o rótulo ao índice que será gerado.
                    if (!rot.isEmpty()) {
                        // o rótulo deve apontar para o índice que o próximo push vai gerar,
                        // mas pode haver casos em que a linha tenha rotulo E instr; neste caso:
                        // queremos que rotulo aponte para este instr (que ainda não foi adicionado),
                        // então definimos labels[rot] = program.size() (índice futuro).
                        labels.put(rot, program.size());
                    }

                    // adiciona a instrução ao program
                    program.add(new Instr(op, l.getVar1(), l.getVar2()));
                } else {
                    // linha sem instrução mas com rótulo: por convenção do Python o rotulo
                    // deve apontar para a próxima instrução; então guardamos agora esse rotulo
                    // apontando para o índice futuro (program.size()).
                    if (!rot.isEmpty()) {
                        labels.put(rot, program.size());
                    }
                }
            }

            // Agora temos 'program' e 'labels' que espelham a estrutura do Python.
            // Inicializar memória e pilha
            memoria = new ArrayList<>();
            s = -1;
            ensureMemorySize(2000); // mesmo comportamento do Python original (2000 posições)

            long steps = 0;
            boolean executando = true;

            int i = 0; // índice em 'program' (sem linhas NULL)
            while (i < program.size() && executando) {
                if (++steps > STEP_LIMIT) {
                    System.out.println("Limite de passos excedido (" + STEP_LIMIT + "). Possível loop infinito.");
                    break;
                }

                Instr inst = program.get(i);
                String op = (inst.op == null ? "" : inst.op.trim().toUpperCase());
                String a = (inst.a == null ? "" : inst.a.trim());
                String b = (inst.b == null ? "" : inst.b.trim());

                if (DEBUG) {
                    System.out.printf("[DEBUG] i=%d op=%s a=%s b=%s s=%d\n", i, op, a, b, s);
                }

                int i_anterior = i; // para decidir incremento automático (igual ao step() Python)
                // execute
                switch (op) {
                    case "START":
                        s = -1;
                        break;

                    case "HLT":
                        executando = false;
                        this.guiPrintln("\nExecução terminada (HLT).");
                        break;

                    case "ALLOC": {
                        int m = paramVal(a);
                        int n = paramVal(b);
                        // copia valores da memória para a pilha (igual Python) — NÃO zera memória
                        for (int k = 0; k < n; k++) {
                            this.s += 1;
                            ensureMemorySize(this.s);
                            // leitura segura de memoria m+k
                            ensureMemorySize(m + k);
                            // armazenar como valor (mesma lógica do Python que guarda strings, aqui usamos int)
                            memoria.set(this.s, new Memoria(this.s, memoria.get(m + k).getValor()));
                        }
                        break;
                    }

                    case "DALLOC": {
                        int m = paramVal(a);
                        int n = paramVal(b);
                        // copia valores da pilha de volta para a memória (ordem inversa) — igual Python
                        for (int k = n - 1; k >= 0; k--) {
                            if (this.s < 0) throw new RuntimeException("Stack underflow em DALLOC");
                            ensureMemorySize(m + k);
                            memoria.set(m + k, new Memoria(m + k, memoria.get(this.s).getValor()));
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
                        memoria.set(addr, new Memoria(addr, val));
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
                            System.out.println("[JMP] rótulo não encontrado: " + a);
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
                                System.out.println("[JMPF] rótulo não encontrado: " + a);
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
                            System.out.println("[CALL] rótulo não encontrado: " + a);
                            pop(); // remove retorno inválido
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
                        // Use JavaFX dialog (igual sua versão). Se rodar sem JavaFX, adaptar para Scanner.
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

                    case "PRN": {
                        int v = pop();
                        guiPrintln(v);
                        break;
                    }

                    case "NULL":
                        // nada
                        break;

                    default:
                        throw new RuntimeException("Instrução inválida: " + op);
                } // fim switch

                // se o código não alterou i (saltos, call, return), então incrementa como em Python
                if (i == i_anterior) {
                    i = i + 1;
                }
            } // fim while

            System.out.println("Execução finalizada. passos=" + steps);
            System.out.println("Saída final:\n" + saida);

        } catch (Exception e) {
            System.out.println("Erro execução: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- helpers ----------

    private void ensureMemorySize(int index) {
        if (index < 0) return;
        while (memoria.size() <= index) {
            int addr = memoria.size();
            memoria.add(new Memoria(addr, 0));
        }
    }

    private void push(int v) {
        s += 1;
        ensureMemorySize(s);
        memoria.set(s, new Memoria(s, v));
    }

    private int pop() {
        if (s < 0) {
            throw new RuntimeException("Pop em pilha vazia!");
        }
        int v = memoria.get(s).getValor();
        s -= 1;
        return v;
    }

    private int top() {
        if (s < 0) throw new RuntimeException("Top em pilha vazia!");
        return memoria.get(s).getValor();
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

    // Imprime para a saída (PRN)
    private void guiPrintln(Object o) {
        this.saida += String.valueOf(o) + "\n";
        System.out.println(o);
    }

    public List<Memoria> getMemoria() { return memoria; }
    public String getSaida() { return saida; }
}

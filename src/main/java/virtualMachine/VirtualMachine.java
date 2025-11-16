package virtualMachine;

import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.util.*;

public class VirtualMachine {

    private final String path;
    private List<Memoria> memoria;
    private int s; // ponteiro da pilha (topo)
    private String saida;
    private final int STACK_OFFSET = 100; // Define o início da Pilha (reservamos 0 a 99 para dados)

    // conjunto de opcodes conhecidos (usado pelo parser)
    private static final Set<String> OPCODES = Set.of(
            "START","LDC","LDV","STR","ADD","SUB","MULT","DIVI",
            "INV","NEG","AND","OR","CME","CMA","CEQ","CDIF","CMEQ","CMAQ",
            "RD","PRN","JMP","JMPF","ALLOC","DALLOC","CALL","RETURN","HLT"
    );

    public VirtualMachine(String path) {
        this.path = path;
    }

    // ---------------------------------------------------------
    // Procura linha pelo rótulo
    // ---------------------------------------------------------
    private int achaLinha(List<LinhaVM> linhas, String rotulo) {
        if (rotulo == null) return -1;
        rotulo = rotulo.trim();
        for (int i = 0; i < linhas.size(); i++) {
            String r = linhas.get(i).getRotulo();
            if (r != null && r.trim().equals(rotulo)) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------
    // Lê o arquivo e converte para objeto LinhaVM (parser robusto)
    // ---------------------------------------------------------
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

                // quebra por tabs primeiro (formato com rótulo na primeira coluna)
                if (raw.contains("\t")) {
                    // mantém raw para preservar tab separation — mas segura com trim
                    String[] parts = raw.split("\t", 2);
                    rotulo = parts[0].trim();

                    if (parts.length == 1 || parts[1].trim().equalsIgnoreCase("NULL")) {
                        instr = "";
                    } else {
                        String resto = parts[1].trim();
                        String[] toks = resto.split("\\s+");
                        if (toks.length >= 1) {
                            instr = toks[0].trim().toUpperCase();
                        }
                        if (toks.length >= 2) var1 = toks[1].trim().replaceAll(",", "");
                        if (toks.length >= 3) var2 = toks[2].trim().replaceAll(",", "");
                    }
                } else {
                    // separa por espaços múltiplos
                    String[] parts = line.split("\\s+");
                    if (parts.length == 0) {
                        continue;
                    }

                    // se primeiro token é um opcode conhecido -> é linha sem rótulo
                    String first = parts[0].trim().toUpperCase();
                    if (OPCODES.contains(first)) {
                        instr = first;
                        if (parts.length >= 2) var1 = parts[1].trim().replaceAll(",", "");
                        if (parts.length >= 3) var2 = parts[2].trim().replaceAll(",", "");
                    } else {
                        // primeiro token NÃO é opcode -> pode ser rótulo (por exemplo "1 NULL" ou "1 CALL 2")
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
                                // caso ambíguo: tratar segundo token como instrução (não reconhecida)
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

        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ---------------------------------------------------------
    // Execução da máquina virtual (com tabela de rótulos)
    // ---------------------------------------------------------
    public void analisaObj() {
        try {
            saida = "";
            List<LinhaVM> linhas = listaLinhas();

            // --- monta tabela de rótulos (rotulo -> índice na lista) ---
            Map<String, Integer> tabelaRotulos = new HashMap<>();
            for (int k = 0; k < linhas.size(); k++) {
                String r = linhas.get(k).getRotulo();
                if (r != null && !r.trim().isEmpty()) {
                    tabelaRotulos.put(r.trim(), k);
                }
            }

            memoria = new ArrayList<>();
            // CORREÇÃO 1: Inicializa a pilha com um offset para não sobrescrever os dados
            s = STACK_OFFSET - 1; // pilha vazia (o topo está logo abaixo do offset)

            // Garante que o segmento de dados até o offset esteja inicializado.
            ensureMemorySize(STACK_OFFSET - 1); 

            for (int i = 0; i < linhas.size(); i++) {
                LinhaVM l = linhas.get(i);
                String op = l.getInstrucao();
                if (op == null) op = "";
                op = op.trim().toUpperCase();

                // linha rótulo-only (NULL) => pular (rótulo já registrado na tabela)
                if (op.isEmpty()) continue;

                String a = (l.getVar1() == null) ? "" : l.getVar1().trim();
                String b = (l.getVar2() == null) ? "" : l.getVar2().trim();

                switch (op) {
                    case "START":
                        memoria.clear();
                        s = STACK_OFFSET - 1;
                        ensureMemorySize(STACK_OFFSET - 1);
                        break;

                    case "LDC":
                        if (!isInteger(a)) { System.out.println("[LDC] arg inválido: " + a); break; }
                        push(Integer.parseInt(a));
                        break;

                    case "LDV":
                        if (!isInteger(a)) { System.out.println("[LDV] arg inválido: " + a); break; }
                        int addrLDV = Integer.parseInt(a);
                        ensureMemorySize(addrLDV);
                        push(memoria.get(addrLDV).getValor());
                        break;

                    case "STR":
                        // CORREÇÃO 2: Usa pop() e verifica a pilha antes
                        if (!isInteger(a)) { System.out.println("[STR] arg inválido: " + a); break; }
                        if (!checkStackAtLeast(1, "STR")) break;
                        int valSTR = pop();
                        int addrSTR = Integer.parseInt(a);
                        ensureMemorySize(addrSTR);
                        memoria.set(addrSTR, new Memoria(addrSTR, valSTR));
                        break;

                    case "ADD": binOp((x,y)->x+y); break;
                    case "SUB": binOp((x,y)->x-y); break;
                    case "MULT": binOp((x,y)->x*y); break;
                    case "DIVI": binOp((x,y)->{
                        if (y==0) { System.out.println("[DIVI] divisão por zero"); return 0; }
                        return x / y;
                    }); break;

                    case "INV":
                        if (!checkStackAtLeast(1,"INV")) break;
                        memoria.set(s, new Memoria(s, -top()));
                        break;

                    case "NEG":
                        if (!checkStackAtLeast(1,"NEG")) break;
                        memoria.set(s, new Memoria(s, 1 - top()));
                        break;

                    case "AND": binOp((x,y)->(x==1&&y==1)?1:0); break;
                    case "OR": binOp((x,y)->(x==1||y==1)?1:0); break;

                    case "CME":  binOp((x,y)-> x<y?1:0); break;
                    case "CMA":  binOp((x,y)-> x>y?1:0); break;
                    case "CEQ":  binOp((x,y)-> x==y?1:0); break;
                    case "CDIF": binOp((x,y)-> x!=y?1:0); break;
                    case "CMEQ": binOp((x,y)-> x<=y?1:0); break;
                    case "CMAQ": binOp((x,y)-> x>=y?1:0); break;

                    case "RD": {
                        // RD permanece o mesmo (lê valor e dá push)
                        TextInputDialog dialog = new TextInputDialog("");
                        dialog.setTitle("Entrada");
                        dialog.setHeaderText("Digite um inteiro:");
                        Optional<String> user = dialog.showAndWait();
                        int val = Integer.parseInt(user.orElse("0"));
                        push(val);
                        break;
                    }

                    case "PRN":
                        // CORREÇÃO 3: Usa pop() para desempilhar ao imprimir
                        if (!checkStackAtLeast(1,"PRN")) break;
                        saida += pop() + "\n";
                        break;

                    case "JMP": {
                        int destino = tabelaRotulos.getOrDefault(a, -1);
                        if (destino < 0) {
                            // fallback usando achaLinha (mantém compatibilidade)
                            destino = achaLinha(linhas, a);
                        }
                        if (destino < 0) System.out.println("[JMP] rótulo não encontrado: " + a);
                        else i = destino - 1;
                        break;
                    }

                    case "JMPF": {
                        if (!checkStackAtLeast(1,"JMPF")) break;
                        int val_jmpf = pop(); // Desempilha
                        if (val_jmpf == 0) {
                            int destino = tabelaRotulos.getOrDefault(a, -1);
                            if (destino < 0) destino = achaLinha(linhas, a);
                            if (destino < 0) System.out.println("[JMPF] rótulo não encontrado: " + a);
                            else i = destino - 1;
                        }
                        break;
                    }

                    case "ALLOC": {
                        if (!isInteger(a) || !isInteger(b)) { System.out.println("[ALLOC] args inválidos: " + a + "," + b); break;}
                        int addr = Integer.parseInt(a);
                        int qtd = Integer.parseInt(b);
                        ensureMemorySize(addr + qtd - 1);
                        for (int j = 0; j < qtd; j++) push(memoria.get(addr + j).getValor());
                        break;
                    }

                    case "DALLOC": {
                        if (!isInteger(a) || !isInteger(b)) { System.out.println("[DALLOC] args inválidos: " + a + "," + b); break;}
                        int addr = Integer.parseInt(a);
                        int qtd = Integer.parseInt(b);
                        ensureMemorySize(addr + qtd - 1);
                        for (int j = qtd - 1; j >= 0; j--) {
                            if (!checkStackAtLeast(1,"DALLOC")) break;
                            memoria.set(addr + j, new Memoria(addr + j, pop())); // Usa pop()
                        }
                        break;
                    }

                    case "CALL": {
                        push(i + 1);
                        int destino = tabelaRotulos.getOrDefault(a, -1);
                        if (destino < 0) destino = achaLinha(linhas, a);
                        if (destino < 0) System.out.println("[CALL] rótulo não encontrado: " + a);
                        else i = destino - 1;
                        break;
                    }

                    case "RETURN":
                        if (!checkStackAtLeast(1,"RETURN")) break;
                        i = pop() - 1;
                        break;

                    case "HLT":
                        return;

                    default:
                        System.out.println("Instrução inválida: " + op);
                } // switch
            } // for

        } catch (Exception e) {
            System.out.println("Erro execução: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ---------------------------------------------------------
    // Garante que memoria tenha pelo menos (index+1) posições
    // ---------------------------------------------------------
    private void ensureMemorySize(int index) {
        if (index < 0) return;
        while (memoria.size() <= index) {
            int addr = memoria.size();
            memoria.add(new Memoria(addr, 0));
        }
    }

    // ---------------------------------------------------------
    // FUNÇÕES AUXILIARES (push/pop/top/binOp)
    // ---------------------------------------------------------
    private void push(int v) {
        s++;
        // Garante que o índice s exista na lista
        ensureMemorySize(s);
        // Atualiza/Cria a célula de memória no endereço s
        memoria.set(s, new Memoria(s, v));
    }

    private int pop() {
        if (s < STACK_OFFSET) {
            System.out.println("Pop em pilha vazia ou invadindo segmento de dados!");
            return 0;
        }
        int v = memoria.get(s).getValor();
        s--;
        return v;
    }

    private int top() {
        if (s < STACK_OFFSET) {
            System.out.println("Top em pilha vazia ou invadindo segmento de dados!");
            return 0;
        }
        return memoria.get(s).getValor();
    }

    private void binOp(Bin op) {
        if (!checkStackAtLeast(2,"binOp")) return;
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
        int have = s - (STACK_OFFSET - 1); // Calcula o tamanho da pilha com base no offset
        if (have < need) {
            System.out.println("Pilha insuficiente para " + instr + " (precisa " + need + ", tem " + have + ")");
            return false;
        }
        return true;
    }

    private interface Bin { int apply(int x, int y); }

    // ---------------------------------------------------------
    public List<Memoria> getMemoria() { return memoria; }
    public String getSaida() { return saida; }
}
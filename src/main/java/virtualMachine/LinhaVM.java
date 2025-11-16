package virtualMachine;

public class LinhaVM {

    private int linha;
    private String rotulo;
    private String instrucao;
    private String var1;
    private String var2;
    private String comentario;

    // Construtor sem-argumento — necessário para "new LinhaVM()" e para frameworks
    public LinhaVM() {
        this.linha = 0;
        this.rotulo = "";
        this.instrucao = "";
        this.var1 = "";
        this.var2 = "";
        this.comentario = "";
    }

    // Construtor completo opcional
    public LinhaVM(int linha, String rotulo, String instrucao, String var1, String var2, String comentario) {
        this.linha = linha;
        this.rotulo = rotulo;
        this.instrucao = instrucao;
        this.var1 = var1;
        this.var2 = var2;
        this.comentario = comentario;
    }

    // Getters
    public int getLinha() { return linha; }
    public String getRotulo() { return rotulo; }
    public String getInstrucao() { return instrucao; }
    public String getVar1() { return var1; }
    public String getVar2() { return var2; }
    public String getComentario() { return comentario; }

    // Setters (necessários se você usa linha.setLinha(...) etc)
    public void setLinha(int linha) { this.linha = linha; }
    public void setRotulo(String rotulo) { this.rotulo = rotulo; }
    public void setInstrucao(String instrucao) { this.instrucao = instrucao; }
    public void setVar1(String var1) { this.var1 = var1; }
    public void setVar2(String var2) { this.var2 = var2; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}

package virtualMachine;

public class Memoria {

    private int endereco;
    private int valor;

    public Memoria(int endereco, int valor) {
        this.endereco = endereco;
        this.valor = valor;
    }

    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }

    public int getEndereco() { return endereco; }
    public void setEndereco(int endereco) { this.endereco = endereco; }
}

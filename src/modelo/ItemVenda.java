package modelo;

public class ItemVenda {
    public Medicamento medicamento;
    public int quantidade;

    public ItemVenda(Medicamento medicamento, int quantidade) {
        this.medicamento = medicamento;
        this.quantidade = quantidade;
    }

    public double getSubtotal() {
        return medicamento.preco * quantidade;
    }
}
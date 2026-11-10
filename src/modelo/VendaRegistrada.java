package modelo;

import java.util.ArrayList;

public class VendaRegistrada {
    public String cupom;
    public String clienteNome;
    public String clienteCpf;
    public double total;
    public String formaPagamento;
    public double valorRecebido;
    public double troco;
    public String operador;
    public ArrayList<ItemVenda> itens;

    public VendaRegistrada(String cupom, String clienteNome, String clienteCpf, double total, String formaPagamento, double valorRecebido, double troco, String operador, ArrayList<ItemVenda> itens) {
        this.cupom = cupom;
        this.clienteNome = clienteNome;
        this.clienteCpf = clienteCpf;
        this.total = total;
        this.formaPagamento = formaPagamento;
        this.valorRecebido = valorRecebido;
        this.troco = troco;
        this.operador = operador;
        this.itens = itens;
    }
}
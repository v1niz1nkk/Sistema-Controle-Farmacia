package modelo;

import java.time.LocalDate;

public class Medicamento {
    public String codigo;
    public String nome;
    public String principioAtivo;
    public double preco;
    public int quantidade;
    public LocalDate validade;
    public boolean controlado;

    public Medicamento(String codigo, String nome, String principioAtivo, double preco, int quantidade, LocalDate validade, boolean controlado) {
        this.codigo = codigo;
        this.nome = nome;
        this.principioAtivo = principioAtivo;
        this.preco = preco;
        this.quantidade = quantidade;
        this.validade = validade;
        this.controlado = controlado;
    }

    public String situacao() {
        if (validade.isBefore(LocalDate.now())) {
            return "VENCIDO";
        }
        if (quantidade == 0) {
            return "SEM ESTOQUE";
        }
        return "OK";
    }
}
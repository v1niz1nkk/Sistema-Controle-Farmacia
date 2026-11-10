import conexao.Conexao;
import modelo.*;
import telas.TelaLogin;
import telas.TelaPrincipal;

import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;

public class Main {

    private static final ArrayList<Usuario> usuarios = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            carregarDadosDoBancoOuPadrao();
            new TelaLogin(usuarios, null).setVisible(true);
        });
    }

    private static void carregarDadosDoBancoOuPadrao() {
        boolean conectou = false;
        try (Connection conn = Conexao.getConexao()) {
            conectou = true;
            usuarios.clear();
            TelaPrincipal.clientes.clear();
            TelaPrincipal.medicamentos.clear();
            TelaPrincipal.historicoVendas.clear();

            // Carrega Usuários
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT nome, login, senha, cargo FROM usuario");
            while (rs.next()) {
                usuarios.add(new Usuario(rs.getString("nome"), rs.getString("login"), rs.getString("senha"), rs.getString("cargo")));
            }

            // Carrega Clientes
            rs = stmt.executeQuery("SELECT nome, cpf, telefone FROM cliente");
            while (rs.next()) {
                TelaPrincipal.clientes.add(new Cliente(rs.getString("nome"), rs.getString("cpf"), rs.getString("telefone")));
            }

            // Carrega Medicamentos
            rs = stmt.executeQuery("SELECT codigo, nome, principio_ativo, preco, quantidade, validade, controlado FROM medicamento");
            while (rs.next()) {
                TelaPrincipal.medicamentos.add(new Medicamento(
                    rs.getString("codigo"), rs.getString("nome"), rs.getString("principio_ativo"),
                    rs.getDouble("preco"), rs.getInt("quantidade"),
                    rs.getDate("validade").toLocalDate(), rs.getBoolean("controlado")
                ));
            }

            // Carrega Histórico de Vendas
            rs = stmt.executeQuery("SELECT numero_cupom, cliente_nome, cliente_cpf, total, forma_pagamento, operador FROM venda ORDER BY id DESC");
            while (rs.next()) {
                TelaPrincipal.historicoVendas.add(new VendaRegistrada(
                    rs.getString("numero_cupom"), rs.getString("cliente_nome"), rs.getString("cliente_cpf"),
                    rs.getDouble("total"), rs.getString("forma_pagamento"),
                    rs.getDouble("total"), 0.0, rs.getString("operador"), new ArrayList<>()
                ));
            }
            System.out.println("Dados carregados com sucesso do MySQL!");
        } catch (Exception e) {
            System.out.println("Erro ao carregar do MySQL: " + e.getMessage());
            System.out.println("Usando dados iniciais em memória.");
        }

        // Caso o banco esteja inacessível ou vazio, usa dados de demonstração
        if (!conectou || usuarios.isEmpty()) {
            usuarios.add(new Usuario("Farmacêutico Proprietário", "admin", "admin123", "ADMIN"));
            usuarios.add(new Usuario("Balconista Padrão", "balconista", "123", "BALCONISTA"));
            TelaPrincipal.clientes.add(new Cliente("Maria Souza", "11122233344", "(85) 99999-1111"));
            TelaPrincipal.clientes.add(new Cliente("João Pereira", "55566677788", "(85) 98888-2222"));
            LocalDate hoje = LocalDate.now();
            TelaPrincipal.medicamentos.add(new Medicamento("M001", "Dipirona 500mg", "Dipirona", 8.50, 50, hoje.plusMonths(10), false));
            TelaPrincipal.medicamentos.add(new Medicamento("M002", "Paracetamol 750mg", "Paracetamol", 12.90, 0, hoje.plusMonths(8), false));
            TelaPrincipal.medicamentos.add(new Medicamento("C001", "Amoxicilina 500mg", "Amoxicilina", 32.00, 20, hoje.plusYears(1), true));
            TelaPrincipal.medicamentos.add(new Medicamento("C002", "Clonazepam 2mg", "Clonazepam", 21.00, 10, hoje.minusDays(10), true));
        }
    }
}
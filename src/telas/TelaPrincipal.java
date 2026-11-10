package telas;

import conexao.Conexao;
import modelo.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Locale;

public class TelaPrincipal extends JFrame {

    public static final ArrayList<Cliente> clientes = new ArrayList<>();
    public static final ArrayList<Medicamento> medicamentos = new ArrayList<>();
    public static final ArrayList<VendaRegistrada> historicoVendas = new ArrayList<>();

    private static final DateTimeFormatter formatoData =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final long[] QR_CODE_BITS = {
            533087359L, 273840449L, 392051037L, 392079197L, 390839645L,
            274364737L, 534074751L, 655104L, 205549742L, 413555256L,
            282887138L, 497686618L, 399213224L, 119433383L, 340410073L,
            52462645L, 216005935L, 455441224L, 214922041L, 296403657L,
            408209399L, 602899L, 533505874L, 273534229L, 390755834L,
            390919577L, 391744340L, 274022859L, 533454996L
    };

    private final Usuario usuarioLogado;
    private final ArrayList<Usuario> usuarios;
    private JTabbedPane abas;
    private DefaultTableModel modeloTabelaClientes;
    private DefaultTableModel modeloTabelaMedicamentos;
    private DefaultTableModel modeloTabelaUsuarios;
    private DefaultTableModel modeloTabelaCarrinho;
    private DefaultTableModel modeloTabelaHistorico;

    private JTextField txtNomeCliente, txtCpfCliente, txtTelCliente;
    private Cliente clienteEmEdicao = null;
    private Medicamento medicamentoEmEdicao = null;

    private Cliente clienteVendaAtual = null;
    private final ArrayList<ItemVenda> carrinhoAtual = new ArrayList<>();
    private JLabel lblStatusClienteVenda;
    private JLabel lblTotalVenda;

    public TelaPrincipal(Usuario usuarioLogado, ArrayList<Usuario> usuarios) {
        super("Sistema de Controle de Farmácia");
        this.usuarioLogado = usuarioLogado;
        this.usuarios = usuarios;

        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel barraTopo = new JPanel(new BorderLayout());
        barraTopo.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        barraTopo.setBackground(new Color(230, 240, 250));

        String cargoTexto = isAdmin() ? "Farmacêutico Proprietário (Administrador)" : "Balconista (Acesso Restrito)";
        JLabel lblUsuario = new JLabel("Logado como: " + usuarioLogado.nome + "  |  Perfil: " + cargoTexto);
        lblUsuario.setFont(new Font("SansSerif", Font.BOLD, 12));

        JButton btnLogout = new JButton("Trocar Usuário (Logout)");
        btnLogout.addActionListener(e -> {
            dispose();
            new TelaLogin(this.usuarios, null).setVisible(true);
        });

        barraTopo.add(lblUsuario, BorderLayout.WEST);
        barraTopo.add(btnLogout, BorderLayout.EAST);

        abas = new JTabbedPane();
        abas.addTab("Medicamentos & Estoque", criarPainelMedicamentos());
        abas.addTab("Clientes", criarPainelClientes());
        abas.addTab("Vendas (PDV)", criarPainelVendas());
        abas.addTab("Histórico de Vendas", criarPainelHistorico());

        if (isAdmin()) {
            abas.addTab("Adicionar Estoque", criarPainelAjusteEstoque());
            abas.addTab("Usuários / Funcionários", criarPainelUsuarios());
        }

        add(barraTopo, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);

        atualizarTabelaClientes();
        atualizarTabelaMedicamentos(medicamentos);
        atualizarTabelaHistorico();
        if (isAdmin()) atualizarTabelaUsuarios();
    }

    private boolean isAdmin() {
        return usuarioLogado.cargo.equals("ADMIN");
    }

    // Persistência no Banco usando a classe Conexao
    private void salvarClienteNoBanco(Cliente c) {
        String sql = "INSERT INTO cliente (nome, cpf, telefone) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE nome = VALUES(nome), telefone = VALUES(telefone)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.nome);
            ps.setString(2, c.cpf);
            ps.setString(3, c.telefone);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao salvar cliente no MySQL: " + e.getMessage());
        }
    }

    private void salvarMedicamentoNoBanco(Medicamento m) {
        String sql = "INSERT INTO medicamento (codigo, nome, principio_ativo, preco, quantidade, validade, controlado) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE nome=VALUES(nome), principio_ativo=VALUES(principio_ativo), preco=VALUES(preco), quantidade=VALUES(quantidade), validade=VALUES(validade), controlado=VALUES(controlado)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.codigo);
            ps.setString(2, m.nome);
            ps.setString(3, m.principioAtivo);
            ps.setDouble(4, m.preco);
            ps.setInt(5, m.quantidade);
            ps.setDate(6, java.sql.Date.valueOf(m.validade));
            ps.setBoolean(7, m.controlado);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao salvar medicamento no MySQL: " + e.getMessage());
        }
    }

    private void atualizarEstoqueNoBanco(String codigo, int novaQuantidade) {
        String sql = "UPDATE medicamento SET quantidade = ? WHERE codigo = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, novaQuantidade);
            ps.setString(2, codigo);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao atualizar estoque no MySQL: " + e.getMessage());
        }
    }

    private void salvarVendaNoBanco(VendaRegistrada v) {
        String sql = "INSERT INTO venda (numero_cupom, data_hora, cliente_nome, cliente_cpf, total, forma_pagamento, operador) VALUES (?, NOW(), ?, ?, ?, ?, ?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.cupom);
            ps.setString(2, v.clienteNome);
            ps.setString(3, v.clienteCpf);
            ps.setDouble(4, v.total);
            ps.setString(5, v.formaPagamento);
            ps.setString(6, v.operador);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao salvar venda no MySQL: " + e.getMessage());
        }
    }

    private void salvarUsuarioNoBanco(Usuario u) {
        String sql = "INSERT INTO usuario (nome, login, senha, cargo) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE nome = VALUES(nome), senha = VALUES(senha), cargo = VALUES(cargo)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.nome);
            ps.setString(2, u.login);
            ps.setString(3, u.senha);
            ps.setString(4, u.cargo);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao salvar usuario no MySQL: " + e.getMessage());
        }
    }

    // Painel de Medicamentos
    private JPanel criarPainelMedicamentos() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(4, 4, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder(isAdmin() ? "Cadastrar / Editar Medicamento" : "Medicamentos (Somente Consulta)"));

        JTextField txtCodigo = new JTextField();
        JTextField txtNome = new JTextField();
        JTextField txtPrincipio = new JTextField();
        JTextField txtPreco = new JTextField();
        JTextField txtQtd = new JTextField();
        JTextField txtValidade = new JTextField();
        JCheckBox chkControlado = new JCheckBox("Controlado");
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnEditar = new JButton("Editar Selecionado");

        form.add(new JLabel("Código:")); form.add(txtCodigo);
        form.add(new JLabel("Nome:")); form.add(txtNome);
        form.add(new JLabel("Princípio Ativo:")); form.add(txtPrincipio);
        form.add(new JLabel("Preço (ex: 12,50):")); form.add(txtPreco);
        form.add(new JLabel("Quantidade:")); form.add(txtQtd);
        form.add(new JLabel("Validade (dd/mm/aaaa):")); form.add(txtValidade);
        form.add(chkControlado); form.add(btnSalvar);
        form.add(new JLabel("")); form.add(btnEditar);

        if (!isAdmin()) {
            txtCodigo.setEnabled(false); txtNome.setEnabled(false); txtPrincipio.setEnabled(false);
            txtPreco.setEnabled(false); txtQtd.setEnabled(false); txtValidade.setEnabled(false);
            chkControlado.setEnabled(false); btnSalvar.setEnabled(false); btnEditar.setEnabled(false);
        }

        JPanel painelBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtBusca = new JTextField(18);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnVerTodos = new JButton("Ver Todos");
        painelBusca.add(new JLabel("Buscar (Nome, Código ou Princípio):"));
        painelBusca.add(txtBusca); painelBusca.add(btnBuscar); painelBusca.add(btnVerTodos);

        String[] cols = {"CÓD", "NOME", "PRINC. ATIVO", "PREÇO", "QTD", "VALIDADE", "CTRL", "SITUAÇÃO"};
        modeloTabelaMedicamentos = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modeloTabelaMedicamentos);

        btnSalvar.addActionListener(e -> {
            try {
                String cod = txtCodigo.getText().trim().toUpperCase();
                String nome = txtNome.getText().trim();
                String principio = txtPrincipio.getText().trim();
                if (cod.isEmpty() || nome.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Código e Nome são obrigatórios.");
                    return;
                }
                double preco = Double.parseDouble(txtPreco.getText().trim().replace(',', '.'));
                int qtd = Integer.parseInt(txtQtd.getText().trim());
                LocalDate val = LocalDate.parse(txtValidade.getText().trim(), formatoData);

                Medicamento medSalvar;
                if (medicamentoEmEdicao == null) {
                    medSalvar = new Medicamento(cod, nome, principio, preco, qtd, val, chkControlado.isSelected());
                    medicamentos.add(medSalvar);
                    JOptionPane.showMessageDialog(this, "Medicamento cadastrado e salvo no MySQL!");
                } else {
                    medicamentoEmEdicao.nome = nome;
                    medicamentoEmEdicao.principioAtivo = principio;
                    medicamentoEmEdicao.preco = preco;
                    medicamentoEmEdicao.quantidade = qtd;
                    medicamentoEmEdicao.validade = val;
                    medicamentoEmEdicao.controlado = chkControlado.isSelected();
                    medSalvar = medicamentoEmEdicao;
                    medicamentoEmEdicao = null;
                    txtCodigo.setEditable(true);
                    btnSalvar.setText("Cadastrar");
                    JOptionPane.showMessageDialog(this, "Medicamento atualizado no MySQL!");
                }
                salvarMedicamentoNoBanco(medSalvar);
                atualizarTabelaMedicamentos(medicamentos);
                txtCodigo.setText(""); txtNome.setText(""); txtPrincipio.setText("");
                txtPreco.setText(""); txtQtd.setText(""); txtValidade.setText("");
                chkControlado.setSelected(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Valores numéricos ou data inválidos. Use dd/mm/aaaa.");
            }
        });

        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha == -1) {
                JOptionPane.showMessageDialog(this, "Selecione um medicamento na tabela.");
                return;
            }
            String cod = (String) modeloTabelaMedicamentos.getValueAt(linha, 0);
            for (Medicamento m : medicamentos) {
                if (m.codigo.equals(cod)) {
                    medicamentoEmEdicao = m;
                    txtCodigo.setText(m.codigo);
                    txtCodigo.setEditable(false);
                    txtNome.setText(m.nome);
                    txtPrincipio.setText(m.principioAtivo);
                    txtPreco.setText(String.format(Locale.US, "%.2f", m.preco).replace('.', ','));
                    txtQtd.setText(String.valueOf(m.quantidade));
                    txtValidade.setText(m.validade.format(formatoData));
                    chkControlado.setSelected(m.controlado);
                    btnSalvar.setText("Salvar Alterações");
                    break;
                }
            }
        });

        btnBuscar.addActionListener(e -> {
            String t = txtBusca.getText().trim().toLowerCase();
            ArrayList<Medicamento> res = new ArrayList<>();
            for (Medicamento m : medicamentos) {
                if (m.nome.toLowerCase().contains(t) || m.codigo.toLowerCase().contains(t) || m.principioAtivo.toLowerCase().contains(t)) {
                    res.add(m);
                }
            }
            atualizarTabelaMedicamentos(res);
        });

        btnVerTodos.addActionListener(e -> {
            txtBusca.setText("");
            atualizarTabelaMedicamentos(medicamentos);
        });

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.NORTH);
        topo.add(painelBusca, BorderLayout.SOUTH);
        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    // Painel de Clientes
    private JPanel criarPainelClientes() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(2, 4, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Cadastrar / Gerenciar Clientes"));

        txtNomeCliente = new JTextField();
        txtCpfCliente = new JTextField();
        txtTelCliente = new JTextField();
        JButton btnSalvar = new JButton("Cadastrar Cliente");
        JButton btnEditar = new JButton("Editar Selecionado");
        if (!isAdmin()) btnEditar.setEnabled(false);

        form.add(new JLabel("Nome:")); form.add(txtNomeCliente);
        form.add(new JLabel("CPF (11 dígitos):")); form.add(txtCpfCliente);
        form.add(new JLabel("Telefone:")); form.add(txtTelCliente);
        form.add(btnEditar); form.add(btnSalvar);

        String[] cols = {"NOME", "CPF", "TELEFONE"};
        modeloTabelaClientes = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modeloTabelaClientes);

        btnSalvar.addActionListener(e -> {
            String n = txtNomeCliente.getText().trim();
            String c = txtCpfCliente.getText().replaceAll("[^0-9]", "");
            String t = txtTelCliente.getText().trim();
            if (n.isEmpty() || c.length() != 11) {
                JOptionPane.showMessageDialog(this, "Preencha nome e CPF com 11 números.");
                return;
            }

            Cliente cliSalvar;
            if (clienteEmEdicao == null) {
                cliSalvar = new Cliente(n, c, t);
                clientes.add(cliSalvar);
                JOptionPane.showMessageDialog(this, "Cliente cadastrado e salvo no MySQL!");
            } else {
                clienteEmEdicao.nome = n;
                clienteEmEdicao.cpf = c;
                clienteEmEdicao.telefone = t;
                cliSalvar = clienteEmEdicao;
                clienteEmEdicao = null;
                btnSalvar.setText("Cadastrar Cliente");
                JOptionPane.showMessageDialog(this, "Cliente atualizado no MySQL!");
            }
            salvarClienteNoBanco(cliSalvar);
            atualizarTabelaClientes();
            txtNomeCliente.setText(""); txtCpfCliente.setText(""); txtTelCliente.setText("");
        });

        btnEditar.addActionListener(e -> {
            int l = tabela.getSelectedRow();
            if (l == -1) {
                JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela.");
                return;
            }
            Cliente c = clientes.get(l);
            clienteEmEdicao = c;
            txtNomeCliente.setText(c.nome);
            txtCpfCliente.setText(c.cpf);
            txtTelCliente.setText(c.telefone);
            btnSalvar.setText("Salvar Alterações");
        });

        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    // Painel de Vendas (PDV)
    private JPanel criarPainelVendas() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel painelCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        painelCliente.setBorder(BorderFactory.createTitledBorder("1. Identificação do Cliente"));
        JTextField txtCpfVenda = new JTextField(12);
        JButton btnVerificarCpf = new JButton("Verificar CPF");
        lblStatusClienteVenda = new JLabel("Nenhum cliente selecionado.");
        lblStatusClienteVenda.setFont(new Font("SansSerif", Font.BOLD, 12));
        painelCliente.add(new JLabel("CPF:")); painelCliente.add(txtCpfVenda);
        painelCliente.add(btnVerificarCpf); painelCliente.add(lblStatusClienteVenda);

        JPanel painelItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        painelItem.setBorder(BorderFactory.createTitledBorder("2. Adicionar Medicamento à Venda"));
        JTextField txtCodMed = new JTextField(10);
        JTextField txtQtdMed = new JTextField("1", 4);
        JButton btnAddItem = new JButton("Adicionar ao Carrinho");
        painelItem.add(new JLabel("Código:")); painelItem.add(txtCodMed);
        painelItem.add(new JLabel("Qtd:")); painelItem.add(txtQtdMed);
        painelItem.add(btnAddItem);

        JPanel topo = new JPanel(new GridLayout(2, 1));
        topo.add(painelCliente); topo.add(painelItem);

        String[] cols = {"CÓDIGO", "MEDICAMENTO", "QTD", "PREÇO UNIT.", "SUBTOTAL"};
        modeloTabelaCarrinho = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaCarrinho = new JTable(modeloTabelaCarrinho);

        JPanel rodape = new JPanel(new BorderLayout());
        lblTotalVenda = new JLabel("Total da Venda: R$ 0,00");
        lblTotalVenda.setFont(new Font("SansSerif", Font.BOLD, 15));
        JButton btnLimpar = new JButton("Cancelar / Limpar");
        JButton btnFinalizar = new JButton("Finalizar Venda (Escolher Pagamento)");
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(btnLimpar); botoes.add(btnFinalizar);
        rodape.add(lblTotalVenda, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);

        btnVerificarCpf.addActionListener(e -> {
            String cpf = txtCpfVenda.getText().replaceAll("[^0-9]", "");
            Cliente achou = null;
            for (Cliente c : clientes) {
                if (c.cpf.equals(cpf)) {
                    achou = c;
                    break;
                }
            }
            if (achou != null) {
                clienteVendaAtual = achou;
                lblStatusClienteVenda.setText("Cliente: " + achou.nome + " | Tel: " + achou.telefone);
                lblStatusClienteVenda.setForeground(new Color(0, 130, 0));
            } else {
                clienteVendaAtual = null;
                lblStatusClienteVenda.setText("CPF não cadastrado.");
                lblStatusClienteVenda.setForeground(Color.RED);
                int r = JOptionPane.showConfirmDialog(this, "CPF não encontrado. Deseja cadastrar agora?", "Cliente Não Encontrado", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    abas.setSelectedIndex(1);
                    txtCpfCliente.setText(cpf);
                    txtNomeCliente.requestFocus();
                }
            }
        });

        btnAddItem.addActionListener(e -> {
            if (clienteVendaAtual == null) {
                JOptionPane.showMessageDialog(this, "Identifique um cliente antes de adicionar itens!");
                return;
            }
            String cod = txtCodMed.getText().trim().toUpperCase();
            Medicamento med = null;
            for (Medicamento m : medicamentos) {
                if (m.codigo.equals(cod)) {
                    med = m;
                    break;
                }
            }

            if (med == null) {
                JOptionPane.showMessageDialog(this, "Medicamento não encontrado com código: " + cod);
                return;
            }
            if (med.situacao().equals("VENCIDO")) {
                JOptionPane.showMessageDialog(this, "Este medicamento está VENCIDO e não pode ser vendido!", "Bloqueio Sanitário", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int qtd = Integer.parseInt(txtQtdMed.getText().trim());
                if (qtd <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantidade deve ser maior que zero.");
                    return;
                }
                int jaNoCarrinho = 0;
                for (ItemVenda it : carrinhoAtual) {
                    if (it.medicamento.codigo.equals(med.codigo)) jaNoCarrinho += it.quantidade;
                }

                if (qtd + jaNoCarrinho > med.quantidade) {
                    JOptionPane.showMessageDialog(this, "Estoque insuficiente! Disponível: " + med.quantidade + " unidades.");
                    return;
                }
                carrinhoAtual.add(new ItemVenda(med, qtd));
                atualizarTabelaCarrinho();
                txtCodMed.setText(""); txtQtdMed.setText("1");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            }
        });

        btnLimpar.addActionListener(e -> {
            carrinhoAtual.clear();
            clienteVendaAtual = null;
            txtCpfVenda.setText("");
            lblStatusClienteVenda.setText("Nenhum cliente selecionado.");
            lblStatusClienteVenda.setForeground(Color.BLACK);
            atualizarTabelaCarrinho();
        });

        btnFinalizar.addActionListener(e -> {
            if (clienteVendaAtual == null || carrinhoAtual.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Identifique o cliente e adicione itens ao carrinho.");
                return;
            }
            double total = 0;
            for (ItemVenda it : carrinhoAtual) total += it.getSubtotal();
            String totalFmt = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", total);

            String[] opcoes = {"PIX (QR Code)", "Dinheiro em Espécie", "Cancelar"};
            int esc = JOptionPane.showOptionDialog(this, "Cliente: " + clienteVendaAtual.nome + "\nTotal: " + totalFmt + "\n\nForma de Pagamento:", "Pagamento",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);

            boolean confirmado = false;
            String forma = "";
            double valorRecebido = total, troco = 0;

            if (esc == 0) {
                confirmado = mostrarModalPix(totalFmt);
                forma = "PIX";
            } else if (esc == 1) {
                String entrada = JOptionPane.showInputDialog(this, "Total: " + totalFmt + "\nDigite o valor recebido em dinheiro (R$):");
                if (entrada != null) {
                    try {
                        valorRecebido = Double.parseDouble(entrada.trim().replace(',', '.'));
                        if (valorRecebido >= total) {
                            troco = valorRecebido - total;
                            confirmado = true;
                            forma = "Dinheiro em Espécie";
                        } else {
                            JOptionPane.showMessageDialog(this, "Valor insuficiente!");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Valor inválido!");
                    }
                }
            }

            if (confirmado) {
                for (ItemVenda it : carrinhoAtual) {
                    it.medicamento.quantidade -= it.quantidade;
                    atualizarEstoqueNoBanco(it.medicamento.codigo, it.medicamento.quantidade);
                }
                atualizarTabelaMedicamentos(medicamentos);

                String cupom = "CUPOM-" + (1000 + historicoVendas.size() + 1);
                VendaRegistrada vr = new VendaRegistrada(cupom, clienteVendaAtual.nome, clienteVendaAtual.cpf, total, forma, valorRecebido, troco, usuarioLogado.nome, new ArrayList<>(carrinhoAtual));
                historicoVendas.add(0, vr);
                salvarVendaNoBanco(vr);
                atualizarTabelaHistorico();

                mostrarNotinhaVenda(vr);

                carrinhoAtual.clear();
                clienteVendaAtual = null;
                txtCpfVenda.setText("");
                lblStatusClienteVenda.setText("Nenhum cliente selecionado.");
                lblStatusClienteVenda.setForeground(Color.BLACK);
                atualizarTabelaCarrinho();
            }
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaCarrinho), BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    // Modal PIX
    private boolean mostrarModalPix(String totalFmt) {
        JDialog d = new JDialog(this, "Pagamento via PIX", true);
        d.setSize(380, 460);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(8, 8));

        JPanel topo = new JPanel(new GridLayout(3, 1));
        topo.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        JLabel l1 = new JLabel("Escaneie o QR Code para pagar", SwingConstants.CENTER);
        l1.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel l2 = new JLabel("Valor: " + totalFmt, SwingConstants.CENTER);
        l2.setFont(new Font("SansSerif", Font.BOLD, 15));
        l2.setForeground(new Color(0, 130, 0));
        JLabel l3 = new JLabel("Aponte a câmera do seu celular:", SwingConstants.CENTER);
        topo.add(l1); topo.add(l2); topo.add(l3);

        JPanel painelQr = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                int tam = 29, margem = 2, total = tam + (margem * 2);
                int escala = Math.min(getWidth(), getHeight()) / total;
                int ox = (getWidth() - (total * escala)) / 2, oy = (getHeight() - (total * escala)) / 2;
                g2.setColor(Color.WHITE);
                g2.fillRect(ox, oy, total * escala, total * escala);
                g2.setColor(Color.BLACK);
                for (int l = 0; l < tam; l++) {
                    long bits = QR_CODE_BITS[l];
                    for (int c = 0; c < tam; c++) {
                        if (((bits >> (28 - c)) & 1L) == 1L) {
                            g2.fillRect(ox + (c + margem) * escala, oy + (l + margem) * escala, escala, escala);
                        }
                    }
                }
            }
        };
        painelQr.setPreferredSize(new Dimension(260, 260));

        final boolean[] res = {false};
        JPanel rod = new JPanel(new FlowLayout());
        JButton btnVoltar = new JButton("Voltar");
        JButton btnConf = new JButton("Confirmar Recebimento do PIX");
        btnVoltar.addActionListener(e -> d.dispose());
        btnConf.addActionListener(e -> { res[0] = true; d.dispose(); });
        rod.add(btnVoltar); rod.add(btnConf);

        d.add(topo, BorderLayout.NORTH);
        d.add(painelQr, BorderLayout.CENTER);
        d.add(rod, BorderLayout.SOUTH);
        d.setVisible(true);
        return res[0];
    }

    // Modal da Notinha
    private void mostrarNotinhaVenda(VendaRegistrada v) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("           FARMÁCIA MODELO LTDA          \n");
        sb.append("       DOCUMENTO AUXILIAR DE VENDA       \n");
        sb.append("=========================================\n");
        sb.append("Cupom: ").append(v.cupom).append("\n");
        sb.append("Cliente: ").append(v.clienteNome).append(" (CPF: ").append(v.clienteCpf).append(")\n");
        sb.append("Atendente: ").append(v.operador).append("\n");
        sb.append("-----------------------------------------\n");
        sb.append(String.format("%-18s %-4s %-8s %-8s\n", "ITEM", "QTD", "UNIT.", "SUBTOTAL"));
        for (ItemVenda it : v.itens) {
            sb.append(String.format("%-18s %-4d R$%-6.2f R$%-6.2f\n",
                    it.medicamento.nome.length() > 18 ? it.medicamento.nome.substring(0, 18) : it.medicamento.nome,
                    it.quantidade, it.medicamento.preco, it.getSubtotal()));
        }
        sb.append("-----------------------------------------\n");
        sb.append(String.format("TOTAL: R$ %.2f\n", v.total));
        sb.append("Forma de Pagamento: ").append(v.formaPagamento).append("\n");
        if (v.formaPagamento.equals("Dinheiro em Espécie")) {
            sb.append(String.format("Valor Recebido: R$ %.2f | Troco: R$ %.2f\n", v.valorRecebido, v.troco));
        }
        sb.append("=========================================\n");
        sb.append("       OBRIGADO PELA PREFERÊNCIA!        \n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Notinha de Venda (Comprovante)", JOptionPane.INFORMATION_MESSAGE);
    }

    // Painel de Histórico
    private JPanel criarPainelHistorico() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        String[] cols = {"CUPOM", "CLIENTE", "CPF", "TOTAL", "PAGAMENTO", "ATENDENTE"};
        modeloTabelaHistorico = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tab = new JTable(modeloTabelaHistorico);

        JButton btnReimprimir = new JButton("Ver Notinha Selecionada");
        btnReimprimir.addActionListener(e -> {
            int l = tab.getSelectedRow();
            if (l == -1) {
                JOptionPane.showMessageDialog(this, "Selecione uma venda na tabela.");
                return;
            }
            mostrarNotinhaVenda(historicoVendas.get(l));
        });

        JPanel rod = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rod.add(btnReimprimir);

        painel.add(new JLabel("Histórico Geral de Vendas Realizadas (Salvo no MySQL):"), BorderLayout.NORTH);
        painel.add(new JScrollPane(tab), BorderLayout.CENTER);
        painel.add(rod, BorderLayout.SOUTH);
        return painel;
    }

    // Painel Ajuste Estoque
    private JPanel criarPainelAjusteEstoque() {
        JPanel painel = new JPanel(new GridBagLayout());
        JPanel caixa = new JPanel(new GridLayout(3, 2, 8, 8));
        caixa.setBorder(BorderFactory.createTitledBorder("Adicionar Estoque"));
        JTextField txtCod = new JTextField();
        JTextField txtQtd = new JTextField();
        JButton btn = new JButton("Adicionar Estoque");
        caixa.add(new JLabel("Código do Medicamento:")); caixa.add(txtCod);
        caixa.add(new JLabel("Quantidade a Adicionar:")); caixa.add(txtQtd);
        caixa.add(new JLabel("")); caixa.add(btn);

        btn.addActionListener(e -> {
            String cod = txtCod.getText().trim().toUpperCase();
            for (Medicamento m : medicamentos) {
                if (m.codigo.equals(cod)) {
                    try {
                        int q = Integer.parseInt(txtQtd.getText().trim());
                        if (q > 0) {
                            m.quantidade += q;
                            atualizarEstoqueNoBanco(m.codigo, m.quantidade);
                            atualizarTabelaMedicamentos(medicamentos);
                            JOptionPane.showMessageDialog(this, m.nome + " agora tem " + m.quantidade + " unidades (salvo no MySQL).");
                            txtCod.setText(""); txtQtd.setText("");
                            return;
                        }
                    } catch (Exception ex) {}
                }
            }
            JOptionPane.showMessageDialog(this, "Medicamento não encontrado ou quantidade inválida.");
        });

        painel.add(caixa);
        return painel;
    }

    // Painel Usuários
    private JPanel criarPainelUsuarios() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel form = new JPanel(new GridLayout(3, 4, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Criar Nova Conta"));
        JTextField txtN = new JTextField(), txtL = new JTextField();
        JPasswordField txtS = new JPasswordField();
        JComboBox<String> cb = new JComboBox<>(new String[]{"BALCONISTA", "ADMIN"});
        JButton btn = new JButton("Criar Conta");
        form.add(new JLabel("Nome:")); form.add(txtN);
        form.add(new JLabel("Login:")); form.add(txtL);
        form.add(new JLabel("Senha:")); form.add(txtS);
        form.add(new JLabel("Cargo:")); form.add(cb);
        form.add(new JLabel("")); form.add(new JLabel("")); form.add(new JLabel("")); form.add(btn);

        String[] cols = {"NOME", "LOGIN", "CARGO"};
        modeloTabelaUsuarios = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tab = new JTable(modeloTabelaUsuarios);

        btn.addActionListener(e -> {
            String n = txtN.getText().trim(), l = txtL.getText().trim(), s = new String(txtS.getPassword()).trim();
            if (!n.isEmpty() && !l.isEmpty() && !s.isEmpty()) {
                Usuario u = new Usuario(n, l, s, (String) cb.getSelectedItem());
                usuarios.add(u);
                salvarUsuarioNoBanco(u);
                atualizarTabelaUsuarios();
                txtN.setText(""); txtL.setText(""); txtS.setText("");
                JOptionPane.showMessageDialog(this, "Conta criada e salva no MySQL!");
            }
        });

        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tab), BorderLayout.CENTER);
        return painel;
    }

    // Métodos utilitários de atualização de tabelas
    private void atualizarTabelaClientes() {
        modeloTabelaClientes.setRowCount(0);
        for (Cliente c : clientes) modeloTabelaClientes.addRow(new Object[]{c.nome, c.cpf, c.telefone});
    }

    private void atualizarTabelaMedicamentos(ArrayList<Medicamento> lista) {
        modeloTabelaMedicamentos.setRowCount(0);
        for (Medicamento m : lista) {
            modeloTabelaMedicamentos.addRow(new Object[]{
                    m.codigo, m.nome, m.principioAtivo, String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", m.preco),
                    m.quantidade, m.validade.format(formatoData), m.controlado ? "SIM" : "não", m.situacao()
            });
        }
    }

    private void atualizarTabelaCarrinho() {
        modeloTabelaCarrinho.setRowCount(0);
        double total = 0;
        for (ItemVenda it : carrinhoAtual) {
            modeloTabelaCarrinho.addRow(new Object[]{
                    it.medicamento.codigo, it.medicamento.nome, it.quantidade,
                    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", it.medicamento.preco),
                    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", it.getSubtotal())
            });
            total += it.getSubtotal();
        }
        lblTotalVenda.setText(String.format(Locale.forLanguageTag("pt-BR"), "Total da Venda: R$ %.2f", total));
    }

    private void atualizarTabelaHistorico() {
        modeloTabelaHistorico.setRowCount(0);
        for (VendaRegistrada v : historicoVendas) {
            modeloTabelaHistorico.addRow(new Object[]{
                    v.cupom, v.clienteNome, v.clienteCpf,
                    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", v.total),
                    v.formaPagamento, v.operador
            });
        }
    }

    private void atualizarTabelaUsuarios() {
        if (modeloTabelaUsuarios == null) return;
        modeloTabelaUsuarios.setRowCount(0);
        for (Usuario u : usuarios) modeloTabelaUsuarios.addRow(new Object[]{u.nome, u.login, u.cargo});
    }
}
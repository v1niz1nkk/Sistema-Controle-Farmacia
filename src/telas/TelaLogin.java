package telas;

import modelo.Usuario;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class TelaLogin extends JFrame {

    private final ArrayList<Usuario> usuarios;
    private final Runnable callbackSucesso;

    public TelaLogin(ArrayList<Usuario> usuarios, Runnable callbackSucesso) {
        super("Login - Sistema de Farmácia");
        this.usuarios = usuarios;
        this.callbackSucesso = callbackSucesso;

        setSize(420, 240);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel painel = new JPanel(new GridLayout(3, 2, 10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        JTextField txtLogin = new JTextField("admin");
        JPasswordField txtSenha = new JPasswordField("admin123");
        JButton btnEntrar = new JButton("Entrar no Sistema");

        painel.add(new JLabel("Usuário:")); painel.add(txtLogin);
        painel.add(new JLabel("Senha:")); painel.add(txtSenha);
        painel.add(new JLabel("")); painel.add(btnEntrar);

        JLabel lblDica = new JLabel("Dica: admin / admin123 ou balconista / 123", SwingConstants.CENTER);
        lblDica.setFont(new Font("SansSerif", Font.ITALIC, 11));

        btnEntrar.addActionListener(e -> {
            String login = txtLogin.getText().trim();
            String senha = new String(txtSenha.getPassword()).trim();
            Usuario autenticado = null;
            for (Usuario u : usuarios) {
                if (u.login.equals(login) && u.senha.equals(senha)) {
                    autenticado = u;
                    break;
                }
            }
            if (autenticado != null) {
                dispose();
                new TelaPrincipal(autenticado, usuarios).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Usuário ou senha incorretos!", "Erro de Login", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(painel, BorderLayout.CENTER);
        add(lblDica, BorderLayout.SOUTH);
    }
}
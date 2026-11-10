package modelo;

public class Usuario {
    public String nome;
    public String login;
    public String senha;
    public String cargo;

    public Usuario(String nome, String login, String senha, String cargo) {
        this.nome = nome;
        this.login = login;
        this.senha = senha;
        this.cargo = cargo;
    }
}
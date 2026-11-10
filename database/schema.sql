CREATE DATABASE IF NOT EXISTS farmacia_db DEFAULT CHARACTER SET utf8mb4;
USE farmacia_db;

CREATE TABLE IF NOT EXISTS cliente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    telefone VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS medicamento (
    codigo VARCHAR(10) PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    principio_ativo VARCHAR(100),
    preco DECIMAL(10, 2) NOT NULL,
    quantidade INT NOT NULL,
    validade DATE NOT NULL,
    controlado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(100) NOT NULL,
    cargo VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS venda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_cupom VARCHAR(20) NOT NULL,
    data_hora DATETIME NOT NULL,
    cliente_nome VARCHAR(100) NOT NULL,
    cliente_cpf VARCHAR(11) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    forma_pagamento VARCHAR(30) NOT NULL,
    operador VARCHAR(100) NOT NULL
);

-- Dados Iniciais
INSERT INTO usuario (nome, login, senha, cargo) VALUES
('Farmacêutico Proprietário', 'admin', 'admin123', 'ADMIN'),
('Balconista Padrão', 'balconista', '123', 'BALCONISTA');

INSERT INTO cliente (nome, cpf, telefone) VALUES
('Maria Souza', '11122233344', '(85) 99999-1111'),
('João Pereira', '55566677788', '(85) 98888-2222');

INSERT INTO medicamento (codigo, nome, principio_ativo, preco, quantidade, validade, controlado) VALUES
('M001', 'Dipirona 500mg', 'Dipirona', 8.50, 50, DATE_ADD(CURDATE(), INTERVAL 10 MONTH), FALSE),
('M002', 'Paracetamol 750mg', 'Paracetamol', 12.90, 0, DATE_ADD(CURDATE(), INTERVAL 8 MONTH), FALSE),
('C001', 'Amoxicilina 500mg', 'Amoxicilina', 32.00, 20, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), TRUE),
('C002', 'Clonazepam 2mg', 'Clonazepam', 21.00, 10, DATE_SUB(CURDATE(), INTERVAL 10 DAY), TRUE);
package io.github.pubsub.rabbitmq.rabbitmqproject.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable; // Importante para serialização em mensagens

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido implements Serializable { // Implementar Serializable é uma boa prática para mensageria
    private String id;
    private String nomeCliente;
    private String produto;
    private double valor;
    private String status; // Ex: "RECEBIDO", "PROCESSADO", "EM_TRANSPORTE", "ENTREGUE"
}
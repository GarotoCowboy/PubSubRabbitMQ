package io.github.pubsub.rabbitmq.rabbitmqproject.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido implements Serializable {
    private String id;
    private String nomeCliente;
    private String produto;
    private double valor;
    private String status;
}
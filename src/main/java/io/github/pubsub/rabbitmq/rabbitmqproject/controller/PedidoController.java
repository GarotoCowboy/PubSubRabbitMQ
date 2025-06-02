package io.github.pubsub.rabbitmq.rabbitmqproject.controller;

import io.github.pubsub.rabbitmq.rabbitmqproject.model.Pedido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private StreamBridge streamBridge;

    @PostMapping
    public ResponseEntity<String> criarPedido(@RequestBody Pedido pedido) {
        pedido.setId(UUID.randomUUID().toString());
        pedido.setStatus("RECEBIDO");

        System.out.println("------------------------------------------------------------");
        System.out.println("[CONTROLLER] Recebido novo pedido: Cliente " + pedido.getNomeCliente() + ", Produto: " + pedido.getProduto());
        boolean enviado = streamBridge.send("enviarPedidoParaProcessamento-out-0", pedido);

        if (enviado) {
            System.out.println("Pedido enviado para processamento: " + pedido.getId());
            System.out.println("------------------------------------------------------------");
            return ResponseEntity.ok("Pedido recebido e enviado para processamento: " + pedido.getId());
        } else {
            //System.err.println("Falha ao enviar pedido para processamento: " + pedido.getId());
            System.err.println("[CONTROLLER] Falha ao enviar pedido ID " + pedido.getId() + " para processamento.");
            System.out.println("------------------------------------------------------------");
            return ResponseEntity.status(500).body("Falha ao enviar pedido para processamento.");
        }
    }
    @NotBlank
    private String nomeCliente;

    @NotBlank
    private String produto;

    @Positive
    private double valor;

}
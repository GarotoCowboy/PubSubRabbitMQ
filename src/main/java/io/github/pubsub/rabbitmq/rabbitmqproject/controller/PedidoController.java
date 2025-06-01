package io.github.pubsub.rabbitmq.rabbitmqproject.controller;

import io.github.pubsub.rabbitmq.rabbitmqproject.model.Pedido;
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
    private StreamBridge streamBridge; // Utilitário para enviar mensagens programaticamente

    @PostMapping
    public ResponseEntity<String> criarPedido(@RequestBody Pedido pedido) {
        pedido.setId(UUID.randomUUID().toString());
        pedido.setStatus("RECEBIDO");

        // Envia para o binding 'enviarPedidoParaProcessamento-out-0'
        // que está mapeado para 'fila-processamento-pedidos'
        boolean enviado = streamBridge.send("enviarPedidoParaProcessamento-out-0", pedido);

        if (enviado) {
            System.out.println("Pedido enviado para processamento: " + pedido.getId());
            return ResponseEntity.ok("Pedido recebido e enviado para processamento: " + pedido.getId());
        } else {
            System.err.println("Falha ao enviar pedido para processamento: " + pedido.getId());
            return ResponseEntity.status(500).body("Falha ao enviar pedido para processamento.");
        }
    }
}
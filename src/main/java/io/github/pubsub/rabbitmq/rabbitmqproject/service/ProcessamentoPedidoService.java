package io.github.pubsub.rabbitmq.rabbitmqproject.service;

import io.github.pubsub.rabbitmq.rabbitmqproject.model.Pedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class ProcessamentoPedidoService {

    @Autowired
    private StreamBridge streamBridge;

    // Consome da 'fila-processamento-pedidos' através do binding 'receberPedidoParaProcessamento-in-0'
    @Bean
    public Consumer<Pedido> receberPedidoParaProcessamento() {
        return pedido -> {
            System.out.println("PROCESSANDO Pedido ID: " + pedido.getId() + ", Cliente: " + pedido.getNomeCliente());

            // Simula o processamento
            try {
                Thread.sleep(2000); // Simula trabalho
                pedido.setStatus("PROCESSADO");
                System.out.println("Pedido ID: " + pedido.getId() + " PROCESSADO.");

                // 1. Envia para a fila de transporte
                // Binding: 'enviarPedidoParaTransporte-out-0' -> 'fila-transporte-pedidos'
                streamBridge.send("enviarPedidoParaTransporte-out-0", pedido);
                System.out.println("Pedido ID: " + pedido.getId() + " enviado para TRANSPORTE.");

                // 2. Publica evento "pedido-processado"
                // Binding: 'publicarEventoPedidoProcessado-out-0' -> exchange 'eventos-pedidos' com routing key 'pedido-processado'
                streamBridge.send("publicarEventoPedidoProcessado-out-0", pedido);
                System.out.println("Evento PEDIDO_PROCESSADO publicado para Pedido ID: " + pedido.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Processamento interrompido para o pedido ID: " + pedido.getId());
                // Considere relançar uma runtime exception se isso deve ir para DLQ
                throw new RuntimeException("Processamento interrompido", e);
            } catch (Exception e) {
                System.err.println("Erro ao processar o pedido ID: " + pedido.getId() + " - " + e.getMessage());
                // Relançar para que o binder lide com retentativas/DLQ
                throw new RuntimeException("Falha no processamento do pedido " + pedido.getId(), e);
            }

        };
    }
}
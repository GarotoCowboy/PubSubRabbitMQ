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


    @Bean
    public Consumer<Pedido> receberPedidoParaProcessamento() {
        return pedido -> {
            System.out.println("PROCESSANDO Pedido ID: " + pedido.getId() + ", Cliente: " + pedido.getNomeCliente());
            System.out.println("\n>>> [PROCESSAMENTO] Iniciando processamento do Pedido ID: " + pedido.getId() + ", Cliente: " + pedido.getNomeCliente());


            // Simula o processamento
            try {
                Thread.sleep(2000); // Simula trabalho
                pedido.setStatus("PROCESSADO");
                System.out.println("Pedido ID: " + pedido.getId() + " PROCESSADO.");

                // Binding: 'enviarPedidoParaTransporte-out-0' -> 'fila-transporte-pedidos'
                streamBridge.send("enviarPedidoParaTransporte-out-0", pedido);
                System.out.println("Pedido ID: " + pedido.getId() + " enviado para TRANSPORTE.");

                // Binding: 'publicarEventoPedidoProcessado-out-0' -> exchange 'eventos-pedidos' com routing key 'pedido-processado'
                streamBridge.send("publicarEventoPedidoProcessado-out-0", pedido);
                System.out.println("[PROCESSAMENTO] Evento 'pedido-processado' publicado para Pedido ID: " + pedido.getId());
                System.out.println("------------------------------------------------------------");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Processamento interrompido para o pedido ID: " + pedido.getId());
                throw new RuntimeException("Processamento interrompido", e);
            } catch (Exception e) {
                System.err.println("Erro ao processar o pedido ID: " + pedido.getId() + " - " + e.getMessage());
                throw new RuntimeException("Falha no processamento do pedido " + pedido.getId(), e);
            }

        };
    }
}
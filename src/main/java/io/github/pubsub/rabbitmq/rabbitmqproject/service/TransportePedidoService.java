package io.github.pubsub.rabbitmq.rabbitmqproject.service;

import io.github.pubsub.rabbitmq.rabbitmqproject.model.Pedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class TransportePedidoService {

    @Autowired
    private StreamBridge streamBridge;

    @Bean
    public Consumer<Pedido> receberPedidoParaTransporte() {
        return pedido -> {
            System.out.println("TRANSPORTANDO Pedido ID: " + pedido.getId() + ", Produto: " + pedido.getProduto());

            // Simula o transporte/entrega
            try {
                Thread.sleep(3000); // Simula trabalho
                pedido.setStatus("ENTREGUE");
                System.out.println("Pedido ID: " + pedido.getId() + " ENTREGUE.");

                streamBridge.send("publicarEventoPedidoEntregue-out-0", pedido);
                System.out.println("Evento PEDIDO_ENTREGUE publicado para Pedido ID: " + pedido.getId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Transporte interrompido para o pedido ID: " + pedido.getId());
            } catch (Exception e) {
                System.err.println("Erro no transporte do pedido ID: " + pedido.getId() + " - " + e.getMessage());
            }
        };
    }
}
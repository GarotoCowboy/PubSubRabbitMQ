package io.github.pubsub.rabbitmq.rabbitmqproject.service;

import io.github.pubsub.rabbitmq.rabbitmqproject.model.Pedido;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.time.LocalDateTime;

@Service
public class AuditoriaService {

    // Consome do exchange 'eventos-pedidos' através do binding 'ouvirEventosAuditoria-in-0'
    // O binding key 'pedido-processado,pedido-entregue' garante que ele receba ambos os eventos.
    @Bean
    public Consumer<Message<Pedido>> ouvirEventosAuditoria() {
        return message -> {
            Pedido pedido = message.getPayload();
            String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");
            String logMessage = String.format("[%s] AUDITORIA: Pedido ID %s. Evento: %s. Status: %s. Cliente: %s. Valor: %.2f",
                    LocalDateTime.now(),
                    pedido.getId(),
                    routingKey,
                    pedido.getStatus(),
                    pedido.getNomeCliente(),
                    pedido.getValor());
            System.out.println(logMessage);
            // Em um cenário real, você gravaria isso em um log, banco de dados, etc.
        };
    }
}
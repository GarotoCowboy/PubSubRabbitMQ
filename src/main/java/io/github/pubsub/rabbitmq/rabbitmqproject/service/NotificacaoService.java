package io.github.pubsub.rabbitmq.rabbitmqproject.service;

import io.github.pubsub.rabbitmq.rabbitmqproject.model.Pedido;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class NotificacaoService {

    @Bean
    public Consumer<Message<Pedido>> ouvirEventosNotificacao() {
        return message -> {
            Pedido pedido = message.getPayload();
            String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");

            if ("pedido-processado".equals(routingKey)) {
                System.out.println("NOTIFICAÇÃO [Cliente]: Olá " + pedido.getNomeCliente() +
                        ", seu pedido " + pedido.getId() + " foi processado e está sendo preparado para envio!");
            } else if ("pedido-entregue".equals(routingKey)) {
                System.out.println("NOTIFICAÇÃO [Cliente]: Olá " + pedido.getNomeCliente() +
                        ", seu pedido " + pedido.getId() + " foi entregue! Aproveite seu produto: " + pedido.getProduto());
            } else {
                System.out.println("NOTIFICAÇÃO [Evento Desconhecido]: " + pedido.getId() + " Status: " + pedido.getStatus() + " RoutingKey: " + routingKey);
            }
        };
    }
}
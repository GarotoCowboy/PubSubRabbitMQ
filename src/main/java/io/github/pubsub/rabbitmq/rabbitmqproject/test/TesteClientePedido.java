package io.github.pubsub.rabbitmq.rabbitmqproject.test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.springframework.http.*; // Adicionado para HttpStatus
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TesteClientePedido {

    private final static String EXCHANGE_NAME = "eventos-pedidos";
    private static String pedidoIdCriado = null;
    // CountDownLatch para esperar por 2 eventos (processado e entregue) para o pedido criado
    private static CountDownLatch eventosEsperadosLatch;

    public static void main(String[] args) {
        System.out.println("[CLIENTE HTTP] Iniciando envio do pedido...");
        enviarPedido();

        if (pedidoIdCriado != null) {
            System.out.println("[CLIENTE HTTP] Pedido ID " + pedidoIdCriado + " criado com sucesso. Iniciando listener de eventos...");
            eventosEsperadosLatch = new CountDownLatch(2); // Espera por 2 eventos (processado e entregue)
            ouvirEventos();
        } else {
            System.err.println("[CLIENTE HTTP] Falha ao criar o pedido ou obter o ID. Listener de eventos não será iniciado.");
        }
        System.out.println("[CLIENTE] Processo cliente finalizado.");
    }

    private static void enviarPedido() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/pedidos";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> pedidoBody = new HashMap<>();
        pedidoBody.put("nomeCliente", "Cliente Testador VIP");
        pedidoBody.put("produto", "Produto de Teste Avançado");
        pedidoBody.put("valor", 999.99);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(pedidoBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("[CLIENTE HTTP] Pedido enviado via cliente Java!");
            System.out.println("[CLIENTE HTTP] Status da Resposta: " + response.getStatusCode());
            System.out.println("[CLIENTE HTTP] Corpo da Resposta: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                pedidoIdCriado = extrairPedidoIdDaResposta(response.getBody());
            }

        } catch (Exception e) {
            System.err.println("[CLIENTE HTTP] Erro ao enviar pedido: " + e.getMessage());
        }
    }

    private static String extrairPedidoIdDaResposta(String responseBody) {
        // Exemplo de corpo da resposta: "Pedido recebido e enviado para processamento: SEU_UUID_AQUI"
        // Usaremos regex para extrair o UUID de forma mais robusta.
        Pattern pattern = Pattern.compile("([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$");
        Matcher matcher = pattern.matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        System.err.println("[CLIENTE HTTP] Não foi possível extrair o ID do pedido da resposta: " + responseBody);
        return null;
    }

    private static void ouvirEventos() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // Se seu RabbitMQ tiver credenciais diferentes do padrão guest/guest, descomente e ajuste:
        // factory.setUsername("guest");
        // factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Garante que o exchange existe (é idempotente, então não há problema em declarar de novo)
            // O tipo "topic" deve corresponder ao que sua aplicação principal usa.
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true); // true para durável

            // Declara uma fila exclusiva, não-durável, auto-delete com um nome gerado pelo servidor
            String queueName = channel.queueDeclare().getQueue();
            System.out.println("[CLIENTE AMQP] Fila temporária '" + queueName + "' criada para ouvir eventos.");
            channel.queueBind(queueName, EXCHANGE_NAME, "pedido-processado");
            channel.queueBind(queueName, EXCHANGE_NAME, "pedido-entregue");

            System.out.println("[CLIENTE AMQP] Aguardando eventos do RabbitMQ para o pedido ID: " + pedidoIdCriado + "...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String routingKey = delivery.getEnvelope().getRoutingKey();

                // Verifica se a mensagem é relevante para o pedido

                if (messageBody.contains("\"id\":\"" + pedidoIdCriado + "\"")) {
                    System.out.println("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                    System.out.println("[CLIENTE AMQP] ATUALIZAÇÃO RELEVANTE RECEBIDA PARA PEDIDO: " + pedidoIdCriado);
                    System.out.println("[CLIENTE AMQP] Evento (Routing Key): " + routingKey);
                    System.out.println("[CLIENTE AMQP] Conteúdo da Mensagem: " + messageBody);
                    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
                    eventosEsperadosLatch.countDown();
                } else {
                    // System.out.println("[CLIENTE AMQP] Evento recebido para outro pedido (RK: " + routingKey + "), ignorando.");
                }
            };

            // Inicia a fila
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                System.out.println("[CLIENTE AMQP] Consumidor cancelado: " + consumerTag);
            });
            boolean todosEventosChegaram = eventosEsperadosLatch.await(60, TimeUnit.SECONDS);

            if (todosEventosChegaram) {
                System.out.println("[CLIENTE AMQP] Ambos os eventos ('processado' e 'entregue') para o pedido " + pedidoIdCriado + " foram recebidos.");
            } else {
                System.out.println("[CLIENTE AMQP] Timeout: Nem todos os eventos para o pedido " + pedidoIdCriado + " foram recebidos em 60 segundos. Verifique os logs da aplicação principal.");
            }

        } catch (Exception e) {
            System.err.println("[CLIENTE AMQP] Erro durante a escuta de eventos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
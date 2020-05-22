package uk.gov.ons.census.fwmtadapter.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import uk.gov.ons.census.fwmtadapter.client.ExceptionManagerClient;
import uk.gov.ons.census.fwmtadapter.messaging.ManagedMessageRecoverer;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;

@Configuration
public class MessageConsumerConfig {
  private final ExceptionManagerClient exceptionManagerClient;
  private final RabbitTemplate rabbitTemplate;
  private final ConnectionFactory connectionFactory;

  @Value("${messagelogging.logstacktraces}")
  private boolean logStackTraces;

  @Value("${queueconfig.consumers}")
  private int consumers;

  @Value("${queueconfig.retry-attempts}")
  private int retryAttempts;

  @Value("${queueconfig.retry-delay}")
  private int retryDelay;

  @Value("${queueconfig.quarantine-exchange}")
  private String quarantineExchange;

  @Value("${queueconfig.action-field-queue}")
  private String actionFieldQueue;

  @Value("${queueconfig.case-updated-queue}")
  private String caseUpdatedQueue;

  public MessageConsumerConfig(
      ExceptionManagerClient exceptionManagerClient,
      RabbitTemplate rabbitTemplate,
      ConnectionFactory connectionFactory) {
    this.exceptionManagerClient = exceptionManagerClient;
    this.rabbitTemplate = rabbitTemplate;
    this.connectionFactory = connectionFactory;
  }

  @Bean
  public MessageChannel actionFieldInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel caseReceiverChannel() {
    return new DirectChannel();
  }

  @Bean
  public AmqpInboundChannelAdapter actionFieldInbound(
      @Qualifier("actionFieldContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("actionFieldInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  public AmqpInboundChannelAdapter caseReceivedInbound(
      @Qualifier("caseReceivedContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("caseReceiverChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  public SimpleMessageListenerContainer actionFieldContainer() {
    return setupListenerContainer(actionFieldQueue, FieldworkFollowup.class);
  }

  @Bean
  public SimpleMessageListenerContainer caseReceivedContainer() {
    return setupListenerContainer(caseUpdatedQueue, ResponseManagementEvent.class);
  }

  private SimpleMessageListenerContainer setupListenerContainer(
      String queueName, Class expectedMessageType) {
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(retryDelay);

    ManagedMessageRecoverer managedMessageRecoverer =
        new ManagedMessageRecoverer(
            exceptionManagerClient,
            expectedMessageType,
            logStackTraces,
            "Fieldwork Adapter",
            queueName,
            quarantineExchange,
            rabbitTemplate);

    RetryOperationsInterceptor retryOperationsInterceptor =
        RetryInterceptorBuilder.stateless()
            .maxAttempts(retryAttempts)
            .backOffPolicy(fixedBackOffPolicy)
            .recoverer(managedMessageRecoverer)
            .build();

    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(consumers);
    container.setChannelTransacted(true);

    container.setAdviceChain(retryOperationsInterceptor);
    return container;
  }

  private AmqpInboundChannelAdapter makeAdapter(
      AbstractMessageListenerContainer listenerContainer, MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    adapter.setHeaderMapper(
        new DefaultAmqpHeaderMapper(null, null) {
          @Override
          public Map<String, Object> toHeadersFromRequest(MessageProperties source) {
            Map<String, Object> headers = new HashMap<>();
            headers.put("contentType", "application/json");
            return headers;
          }
        });
    return adapter;
  }
}

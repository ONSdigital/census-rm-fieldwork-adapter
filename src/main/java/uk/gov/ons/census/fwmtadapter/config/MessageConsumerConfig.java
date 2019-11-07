package uk.gov.ons.census.fwmtadapter.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import uk.gov.ons.census.fwmtadapter.client.ExceptionManagerClient;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;

@Configuration
public class MessageConsumerConfig {
  private final ExceptionManagerClient exceptionManagerClient;
  private final RabbitTemplate rabbitTemplate;
  private final ConnectionFactory defaultConnectionFactory;
  private final ConnectionFactory fieldConnectionFactory;

  @Value("${messagelogging.logstacktraces}")
  private boolean logStackTraces;

  @Value("${queueconfig.consumers}")
  private int consumers;

  @Value("${queueconfig.retry-attempts}")
  private int retryAttempts;

  @Value("${queueconfig.retry-delay}")
  private int retryDelay;

  @Value("${queueconfig.retry-exchange}")
  private String retryExchange;

  @Value("${queueconfig.quarantine-exchange}")
  private String quarantineExchange;

  @Value("${queueconfig.action-field-queue}")
  private String actionFieldQueue;

  @Value("${queueconfig.refusal-queue}")
  private String refusalQueue;

  @Value("${queueconfig.invalid-address-inbound-queue}")
  private String invalidAddressInboundQueue;

  @Value("${queueconfig.uac-updated-queue}")
  private String uacUpdatedQueue;

  public MessageConsumerConfig(
      ExceptionManagerClient exceptionManagerClient,
      RabbitTemplate rabbitTemplate,
      @Qualifier("rmConnectionFactory") ConnectionFactory defaultConnectionFactory,
      @Qualifier("fieldConnectionFactory") ConnectionFactory fieldConnectionFactory) {
    this.exceptionManagerClient = exceptionManagerClient;
    this.rabbitTemplate = rabbitTemplate;
    this.defaultConnectionFactory = defaultConnectionFactory;
    this.fieldConnectionFactory = fieldConnectionFactory;
  }

  @Bean
  public MessageChannel invalidAddressInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel actionFieldInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel refusalInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel uacUpdatedInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel fieldInputChannel() {
    return new DirectChannel();
  }

  @Bean
  AmqpInboundChannelAdapter fieldInbound(
      @Qualifier("fieldContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("fieldInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  AmqpInboundChannelAdapter invalidAddressInbound(
      @Qualifier("invalidAddressContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("invalidAddressInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public AmqpInboundChannelAdapter actionFieldInbound(
      @Qualifier("actionFieldContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("actionFieldInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);

    return adapter;
  }

  @Bean
  public AmqpInboundChannelAdapter refusalInbound(
      @Qualifier("refusalContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("refusalInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public AmqpInboundChannelAdapter uacUpdatedInbound(
      @Qualifier("uacUpdatedContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("uacUpdatedInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public SimpleMessageListenerContainer fieldContainer() {
    return setupListenerContainerWithConnectionFactory(
        "FIELD.queue", ResponseManagementEvent.class, fieldConnectionFactory);
  }

  @Bean
  public SimpleMessageListenerContainer actionFieldContainer() {
    return setupListenerContainer(actionFieldQueue, FieldworkFollowup.class);
  }

  @Bean
  public SimpleMessageListenerContainer refusalContainer() {
    return setupListenerContainer(refusalQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer invalidAddressContainer() {
    return setupListenerContainer(invalidAddressInboundQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer uacUpdatedContainer() {
    return setupListenerContainer(uacUpdatedQueue, ResponseManagementEvent.class);
  }

  // Should this take the connectionFactory too?

  //  ADD in quarrantine and delay Exchange into this field_vhost too..
  //  This is another issue if working over multiple vhosts in apps

  private SimpleMessageListenerContainer setupListenerContainer(
      String queueName, Class expectedMessageType) {
    return setupListenerContainerWithConnectionFactory(
        queueName, expectedMessageType, defaultConnectionFactory);
  }

  private SimpleMessageListenerContainer setupListenerContainerWithConnectionFactory(
      String queueName, Class expectedMessageType, ConnectionFactory passedConnectionFactory) {
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(retryDelay);

    //    ManagedMessageRecoverer managedMessageRecoverer =
    //        new ManagedMessageRecoverer(
    //            exceptionManagerClient,
    //            expectedMessageType,
    //            logStackTraces,
    //            "Fieldwork Adapter",
    //            queueName,
    //            retryExchange,
    //            quarantineExchange,
    //            rabbitTemplate);
    //
    //    RetryOperationsInterceptor retryOperationsInterceptor =
    //        RetryInterceptorBuilder.stateless()
    //            .maxAttempts(retryAttempts)
    //            .backOffPolicy(fixedBackOffPolicy)
    //            .recoverer(managedMessageRecoverer)
    //            .build();

    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(passedConnectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(consumers);
    container.setChannelTransacted(true);
    //    container.setAdviceChain(retryOperationsInterceptor);
    return container;
  }
}

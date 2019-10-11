package uk.gov.ons.census.fwmtadapter.config;

import java.util.TimeZone;
import javax.annotation.PostConstruct;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MarshallingMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmtadapter.client.ExceptionManagerClient;
import uk.gov.ons.census.fwmtadapter.messaging.MessageErrorHandler;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;

@Configuration
@EnableScheduling
public class AppConfig {
  @Value("${messagelogging.logstacktraces}")
  private boolean logStackTraces;

  @Value("${queueconfig.action-field-queue}")
  private String actionFieldQueue;

  @Value("${queueconfig.refusal-queue}")
  private String refusalQueue;

  @Value("${queueconfig.invalid-address-inbound-queue}")
  private String invalidAddressInboundQueue;

  @Value("${queueconfig.uac-updated-queue}")
  private String uacUpdatedQueue;

  @Value("${queueconfig.consumers}")
  private int consumers;

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
  public SimpleMessageListenerContainer actionFieldContainer(
      ConnectionFactory connectionFactory, ExceptionManagerClient exceptionManagerClient) {
    return setupListenerContainer(
        connectionFactory, actionFieldQueue, exceptionManagerClient, FieldworkFollowup.class);
  }

  @Bean
  public SimpleMessageListenerContainer refusalContainer(
      ConnectionFactory connectionFactory, ExceptionManagerClient exceptionManagerClient) {
    return setupListenerContainer(
        connectionFactory, refusalQueue, exceptionManagerClient, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer invalidAddressContainer(
      ConnectionFactory connectionFactory, ExceptionManagerClient exceptionManagerClient) {
    return setupListenerContainer(
        connectionFactory,
        invalidAddressInboundQueue,
        exceptionManagerClient,
        ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer uacUpdatedContainer(
      ConnectionFactory connectionFactory, ExceptionManagerClient exceptionManagerClient) {
    return setupListenerContainer(
        connectionFactory, uacUpdatedQueue, exceptionManagerClient, ResponseManagementEvent.class);
  }

  @Bean
  public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  public Jaxb2Marshaller actionInstructionFieldMarshaller() {
    Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
    jaxb2Marshaller.setContextPath("uk.gov.ons.census.fwmtadapter.model.dto.field");
    return jaxb2Marshaller;
  }

  @Bean
  public MarshallingMessageConverter actionInstructionFieldMarshallingMessageConverter(
      Jaxb2Marshaller actionInstructionFieldMarshaller) {
    MarshallingMessageConverter marshallingMessageConverter =
        new MarshallingMessageConverter(actionInstructionFieldMarshaller);
    marshallingMessageConverter.setContentType("text/xml");
    return marshallingMessageConverter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory,
      MarshallingMessageConverter actionInstructionFieldMarshallingMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(actionInstructionFieldMarshallingMessageConverter);
    rabbitTemplate.setChannelTransacted(true);
    return rabbitTemplate;
  }

  @Bean
  public MapperFacade mapperFacade() {
    MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

    return mapperFactory.getMapperFacade();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  private SimpleMessageListenerContainer setupListenerContainer(
      ConnectionFactory connectionFactory,
      String queueName,
      ExceptionManagerClient exceptionManagerClient,
      Class expectedMessageType) {
    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(consumers);
    MessageErrorHandler messageErrorHandler =
        new MessageErrorHandler(
            exceptionManagerClient,
            expectedMessageType,
            logStackTraces,
            "Fieldwork Adapter",
            queueName);
    container.setErrorHandler(messageErrorHandler);
    container.setChannelTransacted(true);
    return container;
  }
}

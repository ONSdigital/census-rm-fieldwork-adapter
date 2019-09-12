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
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;

@Configuration
@EnableScheduling
public class AppConfig {
  @Value("${queueconfig.action-field-queue}")
  private String actionFieldQueue;

  @Value("${queueconfig.receipt-queue}")
  private String receiptQueue;

  @Value("${queueconfig.refusal-queue}")
  private String refusalQueue;

  @Value("${queueconfig.invalid-address-inbound-queue}")
  private String invalidAddressInboundQueue;

  @Value("${queueconfig.consumers}")
  private int consumers;

  @Bean
  public MessageChannel receiptedChannel() {
    return new DirectChannel();
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
  public AmqpInboundChannelAdapter inbound(
      SimpleMessageListenerContainer receiptContainer,
      @Qualifier("receiptedChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(receiptContainer);
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
  public SimpleMessageListenerContainer actionFieldContainer(
      ConnectionFactory connectionFactory, MessageErrorHandler messageErrorHandler) {
    return setupListenerContainer(
        connectionFactory, actionFieldQueue, messageErrorHandler, FieldworkFollowup.class);
  }

  @Bean
  public SimpleMessageListenerContainer refusalContainer(
      ConnectionFactory connectionFactory, MessageErrorHandler messageErrorHandler) {
    return setupListenerContainer(
        connectionFactory, refusalQueue, messageErrorHandler, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer receiptContainer(
      ConnectionFactory connectionFactory, MessageErrorHandler messageErrorHandler) {
    return setupListenerContainer(
        connectionFactory, receiptQueue, messageErrorHandler, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer invalidAddressContainer(
      ConnectionFactory connectionFactory, MessageErrorHandler messageErrorHandler) {
    return setupListenerContainer(
        connectionFactory,
        invalidAddressInboundQueue,
        messageErrorHandler,
        ResponseManagementEvent.class);
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
      MessageErrorHandler messageErrorHandler,
      Class expectedClass) {
    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(consumers);
    messageErrorHandler.setExpectedType(expectedClass);
    container.setErrorHandler(messageErrorHandler);
    return container;
  }
}

package uk.gov.ons.census.fwmtadapter.config;

import static org.springframework.amqp.core.Binding.DestinationType.QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueSetterUpper {
  @Value("${queueconfig.action-field-queue}")
  private String actionFieldQueue;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.adapter-outbound-queue}")
  private String actionOutboundQueue;

  @Value("${queueconfig.receipt-queue}")
  private String receiptQueue;

  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.refusal-queue}")
  private String refusalQueue;

  @Value("${queueconfig.refusal-routing-key}")
  private String refusalRoutingKey;

  @Bean
  public Queue actionFieldQueue() {
    return new Queue(actionFieldQueue, true);
  }

  @Bean
  public Queue actionOutboundQueue() {
    return new Queue(actionOutboundQueue, true);
  }

  @Bean
  public Queue receiptQueue() {
    return new Queue(receiptQueue, true);
  }

  @Bean
  public Queue refusalQueue() {
    return new Queue(refusalQueue, true);
  }

  @Bean
  public DirectExchange outboundExchange() {
    return new DirectExchange(outboundExchange, true, false);
  }

  @Bean
  public Exchange caseEventExchange() {
    return new TopicExchange(caseEventExchange, true, false);
  }

  @Bean
  public Binding bindingActionOutboundQueue() {
    return new Binding(actionOutboundQueue, QUEUE, outboundExchange, "", null);
  }

  @Bean
  public Binding bindingRefusalQueue() {
    return new Binding(refusalQueue, QUEUE, caseEventExchange, refusalRoutingKey, null);
  }
}

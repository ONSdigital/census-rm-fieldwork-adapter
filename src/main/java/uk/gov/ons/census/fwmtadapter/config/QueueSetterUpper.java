package uk.gov.ons.census.fwmtadapter.config;

import static org.springframework.amqp.core.Binding.DestinationType.QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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

  @Bean
  public Queue actionFieldQueue() {
    return new Queue(actionFieldQueue, true);
  }

  @Bean
  public Queue actionOutboundQueue() {
    return new Queue(actionOutboundQueue, true);
  }

  @Bean
  public DirectExchange outboundExchange() {
    return new DirectExchange(outboundExchange, true, false);
  }

  @Bean
  public Binding bindingActionOutboundQueue() {
    return new Binding(actionOutboundQueue, QUEUE, outboundExchange, "", null);
  }
}

package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@MessageEndpoint
public class ActionFieldReceiver {
  private final String outboundExchange;
  private final RabbitTemplate rabbitTemplate;

  public ActionFieldReceiver(RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
  }

  @Transactional
  @ServiceActivator(inputChannel = "actionFieldInputChannel")
  public void receiveMessage(ActionInstruction actionInstruction) {
    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }
}

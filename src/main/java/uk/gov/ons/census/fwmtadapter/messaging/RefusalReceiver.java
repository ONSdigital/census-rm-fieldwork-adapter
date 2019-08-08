package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@MessageEndpoint
public class RefusalReceiver {
  public static final String REFUSED = "REFUSED";

  private final String outboundExchange;
  private final RabbitTemplate rabbitTemplate;

  public RefusalReceiver(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
  }

  @Transactional
  @ServiceActivator(inputChannel = "refusalInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {
    if (event.getEvent().getType() != EventType.REFUSAL_RECEIVED) {
      throw new RuntimeException(
          String.format("Event Type '%s' is invalid!", event.getEvent().getType()));
    }

    // Do not send refusal back to Field if from Field
    if ("FIELD".equalsIgnoreCase(event.getEvent().getChannel())) {
      return;
    }

    ActionCancel actionCancel = new ActionCancel();
    actionCancel.setCaseId(event.getPayload().getRefusal().getCollectionCase().getId());
    actionCancel.setReason(REFUSED);

    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }
}

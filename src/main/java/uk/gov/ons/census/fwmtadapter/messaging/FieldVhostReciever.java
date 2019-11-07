package uk.gov.ons.census.fwmtadapter.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@MessageEndpoint
public class FieldVhostReciever {

  private final RabbitTemplate rabbitTemplate;

  public FieldVhostReciever(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Transactional
  @ServiceActivator(inputChannel = "fieldInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {

    ActionCancel actionCancel = new ActionCancel();
    actionCancel.setCaseId(event.getEvent().getTransactionId());
    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    rabbitTemplate.convertAndSend("adapter-outbound-exchange", "", actionInstruction);
  }
}

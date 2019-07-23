package uk.gov.ons.census.fwmtadapter.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmtadapter.model.dto.Receipt;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@Service
public class ReceiptService {

  public static final String RECEIPTED = "RECEIPTED";
  private final RabbitTemplate rabbitTemplate;
  private final String outboundExchange;
  private final CaseService caseService;

  public ReceiptService(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
      CaseService caseService) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
    this.caseService = caseService;
  }

  public void processReceipt(Receipt receipt) {
    ActionCancel actionCancel = new ActionCancel();
    actionCancel.setCaseId(getCaseId(receipt));
    actionCancel.setReason(RECEIPTED);

    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    System.out.println("Sending action cancelled");
    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private String getCaseId(Receipt receipt) {
    String caseId = receipt.getCaseId();

    // Before being sent to us from pubsub, if the caseId is null, it gets set to 0?
    // So need to check for that? for now checking its length, could see if it's a valid UUID
    if (caseId == null || caseId.isBlank() || caseId.length() < 5) {
      System.out.println("Requesting case Id from case API");
      caseId = caseService.getCaseIdFromQid(receipt.getQuestionnaire_Id());
      System.out.println("Got caseId: " + caseId);
    }

    return caseId;
  }
}

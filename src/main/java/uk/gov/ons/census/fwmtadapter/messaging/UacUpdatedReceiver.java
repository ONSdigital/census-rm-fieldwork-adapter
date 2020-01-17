package uk.gov.ons.census.fwmtadapter.messaging;

import static uk.gov.ons.census.fwmtadapter.utility.QuestionnaireTypeHelper.isContinuationQuestionnaireType;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseContainerDto;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.Uac;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionCancel;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

@MessageEndpoint
public class UacUpdatedReceiver {

  private static final Logger log = LoggerFactory.getLogger(UacUpdatedReceiver.class);

  private final RabbitTemplate rabbitTemplate;
  private final String outboundExchange;
  private final CaseClient caseClient;

  public UacUpdatedReceiver(
      RabbitTemplate rabbitTemplate,
      @Value("${queueconfig.outbound-exchange}") String outboundExchange,
      CaseClient caseClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboundExchange = outboundExchange;
    this.caseClient = caseClient;
  }

  @Transactional
  @ServiceActivator(inputChannel = "uacUpdatedInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {
    if (canIgnoreEvent(event)) return;

    CaseContainerDto caseContainerDto =
        caseClient.getCaseFromCaseId(event.getPayload().getUac().getCaseId());

    ActionCancel actionCancel = new ActionCancel();
    actionCancel.setCaseId(event.getPayload().getUac().getCaseId());
    actionCancel.setAddressType(caseContainerDto.getAddressType());
    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionCancel(actionCancel);

    rabbitTemplate.convertAndSend(outboundExchange, "", actionInstruction);
  }

  private boolean canIgnoreEvent(ResponseManagementEvent event) {
    Uac uac = event.getPayload().getUac();
    if (StringUtils.isEmpty(uac.getCaseId())) {
      log.with("qid", uac.getQuestionnaireId())
          .warn("We would like to cancel the associated case but it's not been linked yet");
      return true;
    }
    return uac.isActive() || isContinuationQuestionnaireType(uac.getQuestionnaireId());
  }
}

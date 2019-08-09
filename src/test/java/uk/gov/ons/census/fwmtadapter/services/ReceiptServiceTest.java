package uk.gov.ons.census.fwmtadapter.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.ons.census.fwmtadapter.services.ReceiptService.RECEIPTED;
import static uk.gov.ons.census.fwmtadapter.util.ReceiptHelper.setUpResponseManagementReceiptEvent;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.model.dto.ReceiptDTO;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

public class ReceiptServiceTest {

  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_QID = "test_qid";

  @Test
  public void testReceiptWithCaseId() {
    // Given
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setCaseId(TEST_CASE_ID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);
    ReceiptService receiptService = new ReceiptService(rabbitTemplate, "exchange_name", null);

    // When
    receiptService.processReceipt(responseManagementEvent);

    // then
    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange_name"), eq(""), argCaptor.capture());
    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getActionCancel().getReason()).isEqualTo(RECEIPTED);
  }

  @Test
  public void testReceiptWithQidAndNoCaseId() {
    // Given
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQuestionnaireId(TEST_QID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseService caseService = mock(CaseService.class);
    when(caseService.getCaseIdFromQid(TEST_QID)).thenReturn(TEST_CASE_ID);

    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);

    ReceiptService receiptService =
        new ReceiptService(rabbitTemplate, "exchange_name", caseService);

    receiptService.processReceipt(responseManagementEvent);

    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange_name"), eq(""), argCaptor.capture());
    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getActionCancel().getReason()).isEqualTo(RECEIPTED);
  }

  @Test(expected = RuntimeException.class)
  public void testReceiptWithJustQidButNotFoundThroughCaseService() {
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQuestionnaireId(TEST_QID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseService caseService = mock(CaseService.class);
    when(caseService.getCaseIdFromQid(TEST_QID)).thenThrow(new RuntimeException());

    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);

    ReceiptService receiptService =
        new ReceiptService(rabbitTemplate, "exchange_name", caseService);

    receiptService.processReceipt(responseManagementEvent);
  }
}

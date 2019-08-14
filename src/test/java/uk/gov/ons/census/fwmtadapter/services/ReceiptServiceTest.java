package uk.gov.ons.census.fwmtadapter.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.ons.census.fwmtadapter.util.ReceiptHelper.setUpResponseManagementReceiptEvent;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.fwmtadapter.client.CaseClient;
import uk.gov.ons.census.fwmtadapter.model.dto.CaseIdAddressTypeDto;
import uk.gov.ons.census.fwmtadapter.model.dto.ReceiptDTO;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;

public class ReceiptServiceTest {
  private static final String TEST_CASE_ID = "test_case_id";
  private static final String TEST_QID = "test_qid";
  private static final String TEST_ADDRESS_TYPE = "test address type";

  @Test
  public void testReceipt() {
    // Given
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQuestionnaireId(TEST_QID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseClient caseClient = mock(CaseClient.class);

    CaseIdAddressTypeDto caseIdAddressTypeDto = new CaseIdAddressTypeDto();
    caseIdAddressTypeDto.setCaseId(TEST_CASE_ID);
    caseIdAddressTypeDto.setAddressType(TEST_ADDRESS_TYPE);

    when(caseClient.getCaseIdAndAddressTypeFromQid(TEST_QID)).thenReturn(caseIdAddressTypeDto);

    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);

    ReceiptService receiptService = new ReceiptService(rabbitTemplate, "exchange_name", caseClient);

    receiptService.processReceipt(responseManagementEvent);

    ArgumentCaptor<ActionInstruction> argCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange_name"), eq(""), argCaptor.capture());
    ActionInstruction actionInstruction = argCaptor.getValue();
    assertThat(actionInstruction.getActionCancel().getCaseId()).isEqualTo(TEST_CASE_ID);
    assertThat(actionInstruction.getActionCancel().getAddressType()).isEqualTo(TEST_ADDRESS_TYPE);
  }

  @Test(expected = RuntimeException.class)
  public void testReceiptWithJustQidButNotFoundThroughCaseService() {
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQuestionnaireId(TEST_QID);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseClient caseClient = mock(CaseClient.class);
    when(caseClient.getCaseIdAndAddressTypeFromQid(TEST_QID)).thenThrow(new RuntimeException());

    ResponseManagementEvent responseManagementEvent =
        setUpResponseManagementReceiptEvent(receiptDTO);

    ReceiptService receiptService = new ReceiptService(rabbitTemplate, "exchange_name", caseClient);

    receiptService.processReceipt(responseManagementEvent);
  }
}

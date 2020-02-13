package uk.gov.ons.census.fwmtadapter.messaging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.fwmtadapter.model.dto.Address;
import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
import uk.gov.ons.census.fwmtadapter.model.dto.Metadata;
import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCloseActionInstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CaseUpdatedReceiverTest {

  @Mock private RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @InjectMocks private CaseUpdatedReceiver underTest;

  @Test
  public void testReceiveCloseDecision() {
    // Given
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("testId");
    collectionCase.setCaseRef("testRef");
    Address address = new Address();
    address.setAddressLevel("U");
    address.setAddressType("test address type");
    collectionCase.setAddress(address);

    Metadata metadata = new Metadata();
    metadata.setFieldDecision(ActionInstructionType.CLOSE);

    Payload payload = new Payload();
    payload.setCollectionCase(collectionCase);
    payload.setMetadata(metadata);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    ArgumentCaptor<FwmtCloseActionInstruction> aiArgumentCaptor =
        ArgumentCaptor.forClass(FwmtCloseActionInstruction.class);
    verify(rabbitTemplate).convertAndSend(eq(outboundExchange), eq(""), aiArgumentCaptor.capture());
    FwmtCloseActionInstruction actualAi = aiArgumentCaptor.getValue();
    assertThat(actualAi.getCaseId()).isEqualTo("testId");
    assertThat(actualAi.getCaseRef()).isEqualTo("testRef");
    assertThat(actualAi.getAddressType()).isEqualTo("test address type");
    assertThat(actualAi.getAddressLevel()).isEqualTo("U");
    assertThat(actualAi.getActionInstruction()).isEqualTo(ActionInstructionType.CLOSE);
  }
}

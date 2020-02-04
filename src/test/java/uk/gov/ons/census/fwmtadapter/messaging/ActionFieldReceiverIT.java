package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import javax.xml.bind.JAXBException;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.fwmtadapter.model.dto.FieldworkFollowup;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.ActionInstructionType;
import uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtCreateActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ActionFieldReceiverIT {
  private static final String ADAPTER_OUTBOUND_QUEUE = "RM.Field";

  @Value("${queueconfig.action-field-queue}")
  private String actionFieldQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(actionFieldQueue);
    rabbitQueueHelper.purgeQueue(ADAPTER_OUTBOUND_QUEUE);
  }

  @Test
  public void testReceiveMessage() throws InterruptedException, IOException {
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);

    EasyRandom easyRandom = new EasyRandom();
    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setSurveyName("CENSUS");
    fieldworkFollowup.setUndeliveredAsAddress(false);
    fieldworkFollowup.setBlankQreReturned(false);

    rabbitQueueHelper.sendMessage(actionFieldQueue, fieldworkFollowup);

    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    assertThat(actualMessage).isNotNull();
    ObjectMapper objectMapper = new ObjectMapper();
    FwmtCreateActionInstruction actionInstruction =
        objectMapper.readValue(actualMessage, FwmtCreateActionInstruction.class);
    assertThat(actionInstruction.getActionInstruction()).isEqualTo(ActionInstructionType.CREATE);
    assertThat(actionInstruction)
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup, "townName", "postcode", "organisationName", "oa", "uprn");
    assertThat(actionInstruction.getLatitude())
        .isEqualTo(Double.parseDouble(fieldworkFollowup.getLatitude()));
    assertThat(actionInstruction.getLongitude())
        .isEqualTo(Double.parseDouble((fieldworkFollowup.getLongitude())));
    assertThat(actionInstruction.getAddressLine1()).isEqualTo(fieldworkFollowup.getAddressLine1());
    assertThat(actionInstruction.getAddressLine2()).isEqualTo(fieldworkFollowup.getAddressLine2());
    assertThat(actionInstruction.getAddressLine3()).isEqualTo(fieldworkFollowup.getAddressLine3());

    assertThat(actionInstruction)
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "caseId",
            "caseRef",
            "surveyName",
            "addressType",
            "addressLevel",
            "fieldOfficerId");
    assertThat(actionInstruction.getCeExpectedCapacity())
        .isEqualTo(fieldworkFollowup.getCeExpectedCapacity());
  }

  @Test
  public void testReceiveMessageCE() throws InterruptedException, JAXBException, IOException {
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(ADAPTER_OUTBOUND_QUEUE);

    EasyRandom easyRandom = new EasyRandom();
    FieldworkFollowup fieldworkFollowup = easyRandom.nextObject(FieldworkFollowup.class);
    fieldworkFollowup.setLatitude("-179.99999");
    fieldworkFollowup.setLongitude("179.99999");
    fieldworkFollowup.setSurveyName("CENSUS");
    fieldworkFollowup.setUndeliveredAsAddress(false);
    fieldworkFollowup.setBlankQreReturned(false);
    fieldworkFollowup.setAddressType("CE");
    fieldworkFollowup.setAddressLevel("E");
    fieldworkFollowup.setCeExpectedCapacity(5);
    fieldworkFollowup.setCeActualResponses(0);

    rabbitQueueHelper.sendMessage(actionFieldQueue, fieldworkFollowup);

    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    assertThat(actualMessage).isNotNull();
    ObjectMapper objectMapper = new ObjectMapper();
    FwmtCreateActionInstruction actionInstruction =
        objectMapper.readValue(actualMessage, FwmtCreateActionInstruction.class);
    assertThat(actionInstruction.getActionInstruction()).isEqualTo(ActionInstructionType.CREATE);
    assertThat(actionInstruction)
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup, "townName", "postcode", "organisationName", "oa", "uprn");
    assertThat(actionInstruction.getLatitude())
        .isEqualTo(Double.parseDouble(fieldworkFollowup.getLatitude()));
    assertThat(actionInstruction.getLongitude())
        .isEqualTo(Double.parseDouble(fieldworkFollowup.getLongitude()));
    assertThat(actionInstruction.getAddressLine1()).isEqualTo(fieldworkFollowup.getAddressLine1());
    assertThat(actionInstruction.getAddressLine2()).isEqualTo(fieldworkFollowup.getAddressLine2());
    assertThat(actionInstruction.getAddressLine3()).isEqualTo(fieldworkFollowup.getAddressLine3());

    assertThat(actionInstruction)
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "caseId",
            "caseRef",
            "surveyName",
            "addressType",
            "addressLevel",
            "fieldOfficerId");
    assertThat(actionInstruction.getCeExpectedCapacity())
        .isEqualTo(fieldworkFollowup.getCeExpectedCapacity());
  }
}

package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
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
  public void testReceiveMessage() throws InterruptedException, JAXBException {
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
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);

    assertThat(actionInstruction.getActionRequest().getAddress())
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup, "townName", "postcode", "organisationName", "oa", "arid", "uprn");
    assertThat(actionInstruction.getActionRequest().getAddress().getLatitude())
        .isEqualTo(new BigDecimal(fieldworkFollowup.getLatitude()));
    assertThat(actionInstruction.getActionRequest().getAddress().getLongitude())
        .isEqualTo(new BigDecimal(fieldworkFollowup.getLongitude()));
    assertThat(actionInstruction.getActionRequest().getAddress().getLine1())
        .isEqualTo(fieldworkFollowup.getAddressLine1());
    assertThat(actionInstruction.getActionRequest().getAddress().getLine2())
        .isEqualTo(fieldworkFollowup.getAddressLine2());
    assertThat(actionInstruction.getActionRequest().getAddress().getLine3())
        .isEqualTo(fieldworkFollowup.getAddressLine3());

    assertThat(actionInstruction.getActionRequest())
        .isEqualToComparingOnlyGivenFields(
            fieldworkFollowup,
            "actionPlan",
            "actionType",
            "caseId",
            "caseRef",
            "surveyName",
            "addressType",
            "addressLevel",
            "fieldOfficerId",
            "undeliveredAsAddress",
            "blankQreReturned");
    assertThat(actionInstruction.getActionRequest().getTreatmentId())
        .isEqualTo(fieldworkFollowup.getTreatmentCode());
    assertThat(actionInstruction.getActionRequest().getCeExpectedResponses())
        .isEqualTo(fieldworkFollowup.getCeExpectedCapacity());
  }
}

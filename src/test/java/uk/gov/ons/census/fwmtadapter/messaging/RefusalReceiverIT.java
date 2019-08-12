package uk.gov.ons.census.fwmtadapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
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
import uk.gov.ons.census.fwmtadapter.model.dto.CollectionCase;
import uk.gov.ons.census.fwmtadapter.model.dto.Event;
import uk.gov.ons.census.fwmtadapter.model.dto.EventType;
import uk.gov.ons.census.fwmtadapter.model.dto.Payload;
import uk.gov.ons.census.fwmtadapter.model.dto.Refusal;
import uk.gov.ons.census.fwmtadapter.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.fwmtadapter.model.dto.field.ActionInstruction;
import uk.gov.ons.census.fwmtadapter.util.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class RefusalReceiverIT {
  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.refusal-queue}")
  private String refusalQueue;

  @Value("${queueconfig.refusal-routing-key}")
  private String refusalRoutingKey;

  @Value("${queueconfig.adapter-outbound-queue}")
  private String actionOutboundQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  private EasyRandom easyRandom = new EasyRandom();

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(refusalQueue);
    rabbitQueueHelper.purgeQueue(actionOutboundQueue);
  }

  @Test
  public void testRefusalMessageFromNonFieldChannelEmitsMessageToField()
      throws InterruptedException, JAXBException {
    // Given
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);

    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("123");
    Refusal refusal = new Refusal();
    refusal.setCollectionCase(collectionCase);
    Payload payload = new Payload();
    payload.setRefusal(refusal);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payload);
    Event event = new Event();
    event.setType(EventType.REFUSAL_RECEIVED);
    responseManagementEvent.setEvent(event);

    rabbitQueueHelper.sendMessage(caseEventExchange, refusalRoutingKey, responseManagementEvent);

    // Then
    String actualMessage = rabbitQueueHelper.getMessage(outboundQueue);
    assertThat(actualMessage).isNotNull();
    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader(actualMessage);
    ActionInstruction actionInstruction = (ActionInstruction) unmarshaller.unmarshal(reader);
    assertThat(responseManagementEvent.getPayload().getRefusal().getCollectionCase().getId())
        .isEqualTo(actionInstruction.getActionCancel().getCaseId());
    assertThat("REFUSED").isEqualTo(actionInstruction.getActionCancel().getReason());
  }

  @Test
  public void testRefusalMessageFromFieldChannelDoesNotEmitMessageToField()
      throws InterruptedException {
    // Given
    BlockingQueue<String> outboundQueue = rabbitQueueHelper.listen(actionOutboundQueue);

    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getEvent().setType(EventType.REFUSAL_RECEIVED);
    event.getEvent().setChannel("FIELD");

    // When
    rabbitQueueHelper.sendMessage(caseEventExchange, refusalRoutingKey, event);

    // Then
    assertThat(rabbitQueueHelper.getMessage(outboundQueue)).isNull();
  }
}

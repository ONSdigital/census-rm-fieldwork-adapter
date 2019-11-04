package uk.gov.ons.census.fwmtadapter.config;

import java.util.TimeZone;
import javax.annotation.PostConstruct;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MarshallingMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class AppConfig {
  private String host = "localhost";
  private int port = 35672;
  private String username = "guest";
  private String password = "guest";

  @Bean
  public ConnectionFactory fieldConnectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    connectionFactory.setVirtualHost("/");
    return connectionFactory;
  }

//  @Bean
//  public ConnectionFactory rmVhostConnectionFactory() {
//    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
//    connectionFactory.setUsername(username);
//    connectionFactory.setPassword(password);
//    connectionFactory.setVirtualHost("/");
//
//    return connectionFactory;
//  }

  @Bean
  public AmqpAdmin amqpAdmin(@Qualifier("fieldConnectionFactory")ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  public Jaxb2Marshaller actionInstructionFieldMarshaller() {
    Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
    jaxb2Marshaller.setContextPath("uk.gov.ons.census.fwmtadapter.model.dto.field");
    return jaxb2Marshaller;
  }

  @Bean
  public MarshallingMessageConverter actionInstructionFieldMarshallingMessageConverter(
      Jaxb2Marshaller actionInstructionFieldMarshaller) {
    MarshallingMessageConverter marshallingMessageConverter =
        new MarshallingMessageConverter(actionInstructionFieldMarshaller);
    marshallingMessageConverter.setContentType("text/xml");
    return marshallingMessageConverter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
          @Qualifier("fieldConnectionFactory")ConnectionFactory connectionFactory,
      MarshallingMessageConverter actionInstructionFieldMarshallingMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(actionInstructionFieldMarshallingMessageConverter);
    rabbitTemplate.setChannelTransacted(true);
    return rabbitTemplate;
  }

  @Bean
  public MapperFacade mapperFacade() {
    MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

    return mapperFactory.getMapperFacade();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}

package net.chrisrichardson.ftgo.consumerservice.web;

import net.chrisrichardson.ftgo.common.PersonName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CreateConsumerRequestTest {

  @Test
  public void shouldExposeTheNameMaskedToInitials() {
    CreateConsumerRequest request = new CreateConsumerRequest(new PersonName("John", "Doe"));
    assertEquals("J.", request.getName().getFirstName());
    assertEquals("D.", request.getName().getLastName());
  }

  @Test
  public void shouldMaskAfterTheNameIsReplaced() {
    CreateConsumerRequest request = new CreateConsumerRequest(new PersonName("John", "Doe"));
    request.setName(new PersonName("Mary", "Major"));
    assertEquals("M.", request.getName().getFirstName());
    assertEquals("M.", request.getName().getLastName());
  }

  @Test
  public void shouldNotChangeTheNameItWasGiven() {
    PersonName given = new PersonName("John", "Doe");
    new CreateConsumerRequest(given).getName();
    assertEquals("John", given.getFirstName());
    assertEquals("Doe", given.getLastName());
  }

  @Test
  public void shouldGiveTheSameMaskedNameEveryTime() {
    CreateConsumerRequest request = new CreateConsumerRequest(new PersonName("John", "Doe"));
    assertEquals(request.getName().getFirstName(), request.getName().getFirstName());
    assertEquals(request.getName().getLastName(), request.getName().getLastName());
  }
}

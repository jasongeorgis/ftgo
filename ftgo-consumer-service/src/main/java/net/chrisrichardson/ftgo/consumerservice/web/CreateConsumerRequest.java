package net.chrisrichardson.ftgo.consumerservice.web;

import net.chrisrichardson.ftgo.common.PersonName;

public class CreateConsumerRequest {
  private PersonName name;

  /** The consumer's name, masked for privacy before it goes any further. */
  public PersonName getName() {
    return NameMasking.mask(name);
  }

  public void setName(PersonName name) {
    this.name = name;
  }

  public CreateConsumerRequest(PersonName name) {

    this.name = name;
  }

  private CreateConsumerRequest() {
  }


}

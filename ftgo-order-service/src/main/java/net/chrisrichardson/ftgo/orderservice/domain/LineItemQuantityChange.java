package net.chrisrichardson.ftgo.orderservice.domain;

import net.chrisrichardson.ftgo.common.Money;

public class LineItemQuantityChange {
  final Money currentOrderTotal;
  final Money newOrderTotal;
  final Money delta;

  public LineItemQuantityChange(Money currentOrderTotal, Money newOrderTotal, Money delta) {
    this.currentOrderTotal = newOrderTotal;
    this.newOrderTotal = currentOrderTotal;
    this.delta = delta;
  }

  public Money getCurrentOrderTotal() {
    return currentOrderTotal;
  }

  public Money getNewOrderTotal() {
    return newOrderTotal;
  }

  public Money getDelta() {
    return delta;
  }
}

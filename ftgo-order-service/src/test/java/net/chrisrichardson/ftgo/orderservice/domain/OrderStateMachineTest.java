package net.chrisrichardson.ftgo.orderservice.domain;

import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;
import net.chrisrichardson.ftgo.common.UnsupportedStateTransitionException;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderAuthorized;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderCancelled;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderRejected;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CONSUMER_ID;
import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.chickenVindalooLineItems;
import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.AJANTA_RESTAURANT;
import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.CHICKEN_VINDALOO_PRICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Pins every transition of the order state machine, including the ones it refuses. */
public class OrderStateMachineTest {

  private Order order;

  @Before
  public void setUp() {
    order = Order.createOrder(CONSUMER_ID, AJANTA_RESTAURANT, OrderDetailsMother.DELIVERY_INFORMATION,
            chickenVindalooLineItems()).result;
  }

  private void assertRefused(Runnable transition, OrderState stateAfterRefusal) {
    try {
      transition.run();
      fail("expected the transition to be refused");
    } catch (UnsupportedStateTransitionException e) {
      assertEquals("current state: " + stateAfterRefusal, e.getMessage());
    }
    assertEquals(stateAfterRefusal, order.getState());
  }

  private OrderRevision revisionTo(int quantity) {
    return new OrderRevision(Optional.empty(),
            Collections.singletonList(new RevisedOrderLineItem(quantity, "1")));
  }

  @Test
  public void shouldStartWaitingForApproval() {
    assertEquals(OrderState.APPROVAL_PENDING, order.getState());
  }

  @Test
  public void shouldApproveAPendingOrder() {
    List<OrderDomainEvent> events = order.noteApproved();
    assertEquals(OrderState.APPROVED, order.getState());
    assertEquals(Collections.singletonList(new OrderAuthorized()), events);
  }

  @Test
  public void shouldRefuseToApproveTwice() {
    order.noteApproved();
    assertRefused(order::noteApproved, OrderState.APPROVED);
  }

  @Test
  public void shouldRejectAPendingOrder() {
    List<OrderDomainEvent> events = order.noteRejected();
    assertEquals(OrderState.REJECTED, order.getState());
    assertEquals(1, events.size());
    assertEquals(OrderRejected.class, events.get(0).getClass());
  }

  @Test
  public void shouldRefuseToRejectAnApprovedOrder() {
    order.noteApproved();
    assertRefused(order::noteRejected, OrderState.APPROVED);
  }

  @Test
  public void shouldRefuseToApproveARejectedOrder() {
    order.noteRejected();
    assertRefused(order::noteApproved, OrderState.REJECTED);
  }

  @Test
  public void shouldCancelAnApprovedOrderInTwoSteps() {
    order.noteApproved();

    assertEquals(Collections.emptyList(), order.cancel());
    assertEquals(OrderState.CANCEL_PENDING, order.getState());

    List<OrderDomainEvent> events = order.noteCancelled();
    assertEquals(OrderState.CANCELLED, order.getState());
    assertEquals(1, events.size());
    assertEquals(OrderCancelled.class, events.get(0).getClass());
  }

  @Test
  public void shouldRefuseToCancelAnOrderThatIsNotApproved() {
    assertRefused(order::cancel, OrderState.APPROVAL_PENDING);
  }

  @Test
  public void shouldRefuseToCancelTwice() {
    order.noteApproved();
    order.cancel();
    assertRefused(order::cancel, OrderState.CANCEL_PENDING);
  }

  @Test
  public void shouldRefuseToConfirmACancellationThatWasNeverStarted() {
    order.noteApproved();
    assertRefused(order::noteCancelled, OrderState.APPROVED);
  }

  @Test
  public void shouldRestoreAnOrderWhoseCancellationWasUndone() {
    order.noteApproved();
    order.cancel();

    assertEquals(Collections.emptyList(), order.undoPendingCancel());
    assertEquals(OrderState.APPROVED, order.getState());
    assertEquals(Collections.emptyList(), order.cancel());
  }

  @Test
  public void shouldRefuseToUndoACancellationThatWasNeverStarted() {
    order.noteApproved();
    assertRefused(order::undoPendingCancel, OrderState.APPROVED);
  }

  @Test
  public void shouldRefuseToUndoACancellationThatAlreadyCompleted() {
    order.noteApproved();
    order.cancel();
    order.noteCancelled();
    assertRefused(order::undoPendingCancel, OrderState.CANCELLED);
  }

  @Test
  public void shouldProposeARevisionOfAnApprovedOrder() {
    order.noteApproved();

    ResultWithDomainEvents<LineItemQuantityChange, OrderDomainEvent> result = order.revise(revisionTo(10));

    assertEquals(OrderState.REVISION_PENDING, order.getState());
    assertEquals(CHICKEN_VINDALOO_PRICE.multiply(10), result.result.getNewOrderTotal());
    assertEquals(1, result.events.size());
  }

  @Test
  public void shouldRefuseToReviseAnOrderThatIsNotApproved() {
    assertRefused(() -> order.revise(revisionTo(10)), OrderState.APPROVAL_PENDING);
  }

  @Test
  public void shouldKeepItemsWhenARevisionIsRejected() {
    order.noteApproved();
    order.revise(revisionTo(10));

    assertEquals(Collections.emptyList(), order.rejectRevision());

    assertEquals(OrderState.APPROVED, order.getState());
    assertEquals(OrderDetailsMother.CHICKEN_VINDALOO_ORDER_TOTAL, order.getOrderTotal());
  }

  @Test
  public void shouldApplyItemsWhenARevisionIsConfirmed() {
    order.noteApproved();
    OrderRevision revision = revisionTo(10);
    order.revise(revision);

    List<OrderDomainEvent> events = order.confirmRevision(revision);

    assertEquals(OrderState.APPROVED, order.getState());
    assertEquals(CHICKEN_VINDALOO_PRICE.multiply(10), order.getOrderTotal());
    assertEquals(1, events.size());
  }

  @Test
  public void shouldRefuseToRejectOrConfirmARevisionThatWasNeverProposed() {
    order.noteApproved();
    assertRefused(order::rejectRevision, OrderState.APPROVED);
    assertRefused(() -> order.confirmRevision(revisionTo(10)), OrderState.APPROVED);
  }

  @Test
  public void shouldRefuseToCancelWhileARevisionIsPending() {
    order.noteApproved();
    order.revise(revisionTo(10));
    assertRefused(order::cancel, OrderState.REVISION_PENDING);
  }

  @Test
  public void shouldRefuseToReviseWhileACancellationIsPending() {
    order.noteApproved();
    order.cancel();
    assertRefused(() -> order.revise(revisionTo(10)), OrderState.CANCEL_PENDING);
  }

  @Test
  public void shouldReportEveryRefusalWithTheCurrentState() {
    order.noteApproved();
    order.cancel();
    order.noteCancelled();
    try {
      order.noteApproved();
      fail("a cancelled order cannot be approved");
    } catch (UnsupportedStateTransitionException e) {
      assertTrue(e.getMessage().endsWith("CANCELLED"));
    }
  }
}

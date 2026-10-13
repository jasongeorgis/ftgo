package net.chrisrichardson.ftgo.kitchenservice.domain;

import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;
import net.chrisrichardson.ftgo.common.UnsupportedStateTransitionException;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketLineItem;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketCancelled;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Pins the ticket's cancellation transitions and what each one allows afterwards. */
public class TicketTest {

  private static final long RESTAURANT_ID = 101L;
  private static final long TICKET_ID = 202L;

  private Ticket ticket;

  @Before
  public void setUp() {
    TicketDetails details = new TicketDetails(
            Collections.singletonList(new TicketLineItem("1", "Chicken Vindaloo", 2)));
    ticket = Ticket.create(RESTAURANT_ID, TICKET_ID, details).result;
  }

  /** Places the ticket directly in a state, since no public transition reaches some of them. */
  private void putInState(TicketState state) {
    try {
      Field field = Ticket.class.getDeclaredField("state");
      field.setAccessible(true);
      field.set(ticket, state);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private TicketState stateOf(Ticket subject) {
    try {
      Field field = Ticket.class.getDeclaredField("state");
      field.setAccessible(true);
      return (TicketState) field.get(subject);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private void assertRefused(Callable<List<TicketDomainEvent>> transition, TicketState stateAfterRefusal) {
    try {
      transition.call();
      fail("expected the transition to be refused");
    } catch (UnsupportedStateTransitionException e) {
      assertEquals("current state: " + stateAfterRefusal, e.getMessage());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    assertEquals(stateAfterRefusal, stateOf(ticket));
  }

  @Test
  public void shouldStartWaitingForCreationToBeConfirmed() {
    assertEquals(TicketState.CREATE_PENDING, stateOf(ticket));
  }

  @Test
  public void shouldAwaitAcceptanceOnceCreationIsConfirmed() {
    ticket.confirmCreate();
    assertEquals(TicketState.AWAITING_ACCEPTANCE, stateOf(ticket));
  }

  @Test
  public void shouldRefuseToConfirmCreationTwice() {
    ticket.confirmCreate();
    assertRefused(ticket::confirmCreate, TicketState.AWAITING_ACCEPTANCE);
  }

  @Test
  public void shouldCancelATicketAwaitingAcceptance() {
    putInState(TicketState.AWAITING_ACCEPTANCE);

    assertEquals(Collections.emptyList(), ticket.cancel());

    assertEquals(TicketState.CANCEL_PENDING, stateOf(ticket));
  }

  @Test
  public void shouldCancelAnAcceptedTicket() {
    putInState(TicketState.ACCEPTED);

    assertEquals(Collections.emptyList(), ticket.cancel());

    assertEquals(TicketState.CANCEL_PENDING, stateOf(ticket));
  }

  @Test
  public void shouldCancelATicketThatIsBeingPrepared() {
    putInState(TicketState.PREPARING);

    assertEquals(Collections.emptyList(), ticket.cancel());

    assertEquals(TicketState.CANCEL_PENDING, stateOf(ticket));
  }

  @Test
  public void shouldRefuseToCancelATicketBeforeCreationIsConfirmed() {
    assertRefused(ticket::cancel, TicketState.CREATE_PENDING);
  }

  @Test
  public void shouldRefuseToCancelATicketReadyForPickup() {
    putInState(TicketState.READY_FOR_PICKUP);
    assertRefused(ticket::cancel, TicketState.READY_FOR_PICKUP);
  }

  @Test
  public void shouldRefuseToCancelATicketAlreadyPickedUp() {
    putInState(TicketState.PICKED_UP);
    assertRefused(ticket::cancel, TicketState.PICKED_UP);
  }

  @Test
  public void shouldRefuseToCancelTwice() {
    putInState(TicketState.ACCEPTED);
    ticket.cancel();
    assertRefused(ticket::cancel, TicketState.CANCEL_PENDING);
  }

  @Test
  public void shouldRefuseToCancelACancelledTicket() {
    putInState(TicketState.CANCEL_PENDING);
    ticket.confirmCancel();
    assertRefused(ticket::cancel, TicketState.CANCELLED);
  }

  @Test
  public void shouldRefuseToCancelWhileARevisionIsPending() {
    putInState(TicketState.ACCEPTED);
    ticket.beginReviseOrder(Collections.<RevisedOrderLineItem>emptyList());
    assertRefused(ticket::cancel, TicketState.REVISION_PENDING);
  }

  @Test
  public void shouldAnnounceTheCancellationOnceConfirmed() {
    putInState(TicketState.ACCEPTED);
    ticket.cancel();

    List<TicketDomainEvent> events = ticket.confirmCancel();

    assertEquals(TicketState.CANCELLED, stateOf(ticket));
    assertEquals(1, events.size());
    assertEquals(TicketCancelled.class, events.get(0).getClass());
  }

  @Test
  public void shouldRefuseToConfirmACancellationThatWasNeverStarted() {
    putInState(TicketState.ACCEPTED);
    assertRefused(ticket::confirmCancel, TicketState.ACCEPTED);
  }

  @Test
  public void shouldReturnToAwaitingAcceptanceWhenCancellationIsUndone() {
    putInState(TicketState.AWAITING_ACCEPTANCE);
    ticket.cancel();

    assertEquals(Collections.emptyList(), ticket.undoCancel());

    assertEquals(TicketState.AWAITING_ACCEPTANCE, stateOf(ticket));
  }

  @Test
  public void shouldReturnToAcceptedWhenCancellationIsUndone() {
    putInState(TicketState.ACCEPTED);
    ticket.cancel();

    ticket.undoCancel();

    assertEquals(TicketState.ACCEPTED, stateOf(ticket));
    assertEquals(1, ticket.preparing().size());
  }

  @Test
  public void shouldKeepPreparingWhenCancellationIsUndone() {
    putInState(TicketState.PREPARING);
    ticket.cancel();

    ticket.undoCancel();

    assertEquals(TicketState.PREPARING, stateOf(ticket));
    assertEquals(1, ticket.readyForPickup().size());
    assertEquals(TicketState.READY_FOR_PICKUP, stateOf(ticket));
  }

  @Test
  public void shouldRefuseToUndoACancellationThatWasNeverStarted() {
    putInState(TicketState.PREPARING);
    assertRefused(ticket::undoCancel, TicketState.PREPARING);
  }

  @Test
  public void shouldRefuseToUndoACancellationThatAlreadyCompleted() {
    putInState(TicketState.CANCEL_PENDING);
    ticket.confirmCancel();
    assertRefused(ticket::undoCancel, TicketState.CANCELLED);
  }

  @Test
  public void shouldPrepareThenHandOverAnAcceptedTicket() {
    putInState(TicketState.ACCEPTED);

    assertEquals(1, ticket.preparing().size());
    assertEquals(TicketState.PREPARING, stateOf(ticket));
    assertEquals(1, ticket.readyForPickup().size());
    assertEquals(TicketState.READY_FOR_PICKUP, stateOf(ticket));
    assertEquals(1, ticket.pickedUp().size());
    assertEquals(TicketState.PICKED_UP, stateOf(ticket));
  }

  @Test
  public void shouldRefuseToPrepareATicketThatWasNotAccepted() {
    putInState(TicketState.AWAITING_ACCEPTANCE);
    assertRefused(ticket::preparing, TicketState.AWAITING_ACCEPTANCE);
  }

  @Test
  public void shouldRefuseToHandOverATicketThatIsStillBeingPrepared() {
    putInState(TicketState.PREPARING);
    assertRefused(ticket::pickedUp, TicketState.PREPARING);
  }

  private void assertCancelThenUndoRestores(TicketState state) {
    putInState(state);
    ticket.cancel();
    assertEquals(TicketState.CANCEL_PENDING, stateOf(ticket));
    ticket.undoCancel();
    assertEquals(state, stateOf(ticket));
  }

  @Test
  public void shouldRestoreAwaitingAcceptanceAfterCancelAndUndo() {
    assertCancelThenUndoRestores(TicketState.AWAITING_ACCEPTANCE);
  }

  @Test
  public void shouldRestoreAcceptedAfterCancelAndUndo() {
    assertCancelThenUndoRestores(TicketState.ACCEPTED);
  }

  @Test
  public void shouldRestorePreparingAfterCancelAndUndo() {
    assertCancelThenUndoRestores(TicketState.PREPARING);
  }

  @Test
  public void shouldRemainCancelledAfterAnotherCancelIsRefused() {
    putInState(TicketState.PREPARING);
    ticket.cancel();
    ticket.confirmCancel();
    assertRefused(ticket::cancel, TicketState.CANCELLED);
    assertRefused(ticket::undoCancel, TicketState.CANCELLED);
  }
}

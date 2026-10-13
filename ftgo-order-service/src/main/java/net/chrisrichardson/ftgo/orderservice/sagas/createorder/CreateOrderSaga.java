package net.chrisrichardson.ftgo.orderservice.sagas.createorder;

import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.*;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicketReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateOrderSaga implements SimpleSaga<CreateOrderSagaState> {


  private Logger logger = LoggerFactory.getLogger(getClass());

  private SagaDefinition<CreateOrderSagaState> sagaDefinition;

  public CreateOrderSaga(OrderServiceProxy orderService, ConsumerServiceProxy consumerService, KitchenServiceProxy kitchenService,
                         AccountingServiceProxy accountingService) {
    this.sagaDefinition =
             step()
              .withCompensation(orderService.reject, state -> state.makeRejectOrderCommand())
            .step()
              .invokeParticipant(consumerService.validateOrder, state -> state.makeValidateOrderByConsumerCommand())
            .step()
              .invokeParticipant(kitchenService.create, state -> state.makeCreateTicketCommand())
              .onReply(CreateTicketReply.class, (state, reply) -> state.handleCreateTicketReply(reply))
              .withCompensation(kitchenService.cancel, state -> state.makeCancelCreateTicketCommand())
            .step()
                .invokeParticipant(accountingService.authorize, state -> state.makeAuthorizeCommand())
            .step()
              .invokeParticipant(kitchenService.confirmCreate, state -> state.makeConfirmCreateTicketCommand())
            .step()
              .invokeParticipant(orderService.approve, state -> state.makeApproveOrderCommand())
            .build();

  }


  @Override
  public SagaDefinition<CreateOrderSagaState> getSagaDefinition() {
    return sagaDefinition;
  }


}

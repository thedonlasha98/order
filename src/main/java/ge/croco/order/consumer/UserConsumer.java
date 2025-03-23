package ge.croco.order.consumer;

import ge.croco.order.model.event.UserEvent;
import ge.croco.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "users", concurrency = "3")
    protected void receiveUserEvent(UserEvent userEvent) {
        log.info("Received user event: {}", userEvent);
        if ("USER_DELETED".equals(userEvent.getEventType())) {
            orderService.deleteByUserId(userEvent.getId());
        }
    }
}

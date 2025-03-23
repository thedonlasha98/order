package ge.croco.order.model.event;

import ge.croco.order.enums.EventType;
import ge.croco.order.model.dto.OrderDetails;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent extends OrderDetails {
    private EventType eventType;
    private Instant timestamp;
}
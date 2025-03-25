package ge.croco.order.model.event;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class UserEvent extends UserDetails {
    private String eventType;
    private Instant timestamp;

    public UserEvent(String eventType, Instant timestamp, UserDetails user) {
        super(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
        this.eventType = eventType;
        this.timestamp = timestamp;
    }
}

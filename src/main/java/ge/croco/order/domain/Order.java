package ge.croco.order.domain;

import ge.croco.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderId;

    @Column(unique = true)
    private String externalOrderId;

    private int quantity;

    private BigDecimal price;

    private BigDecimal totalPrice;

    private String product;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private boolean alive;

    private Long userId;

    private LocalDateTime expirationDate;

    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

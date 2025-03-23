package ge.croco.order.model.dto;

import ge.croco.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetails {
    private String orderId;
    private String externalOrderId;
    private Long userId;
    private String product;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private boolean alive;
    private LocalDateTime expirationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

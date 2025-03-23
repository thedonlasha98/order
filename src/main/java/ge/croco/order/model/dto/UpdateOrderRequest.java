package ge.croco.order.model.dto;

import ge.croco.order.enums.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateOrderRequest(
        String product,
        @Min(value = 1) int quantity,
        @NotNull BigDecimal price,
        @NotNull OrderStatus status
) {
}

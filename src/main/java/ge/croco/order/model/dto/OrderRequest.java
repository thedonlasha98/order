package ge.croco.order.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Valid
public record OrderRequest(
        @NotNull String orderId,
        String product,
        @Min(value = 1) int quantity,
        @NotNull BigDecimal price,
        int ttl
) {
}

package ge.croco.order.exception;

import ge.croco.order.enums.OrderStatus;

public class OrderInvalidStatusException extends RuntimeException {
    public OrderInvalidStatusException(OrderStatus status) {
        super("Forbidden operation for order status : " + status.name());
    }
}

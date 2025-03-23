package ge.croco.order.util;

import ge.croco.order.domain.Order;
import ge.croco.order.enums.OrderStatus;
import ge.croco.order.model.dto.UpdateOrderRequest;

import java.util.Set;

public class OrderValidator {

    public static boolean changedTotalPrice(UpdateOrderRequest newOrder, Order currentOrder) {
        return newOrder.quantity() != currentOrder.getQuantity() ||
                newOrder.price().compareTo(currentOrder.getPrice()) != 0;
    }

    public static boolean isPossibleToChangeOrderDetails(Order currentOrder) {
        return Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED).contains(currentOrder.getStatus());
    }

    public static boolean isAliveStatus(OrderStatus status) {
        return !Set.of(OrderStatus.COMPLETED, OrderStatus.DELETED).contains(status);
    }

    public static boolean isValidToDeleteOrder(Order order) {
        return isPossibleToChangeOrderDetails(order);
    }

}

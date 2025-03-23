package ge.croco.order.model.mapper;

import ge.croco.order.domain.Order;
import ge.croco.order.model.dto.OrderDetails;
import ge.croco.order.model.event.OrderEvent;
import ge.croco.order.model.dto.OrderRequest;

import java.time.LocalDateTime;

public class OrderMapper {
    public static Order toEntity(OrderRequest orderRequest) {
        return Order.builder()
                .externalOrderId(orderRequest.orderId())
                .createdAt(LocalDateTime.now())
                .product(orderRequest.product())
                .price(orderRequest.price())
                .quantity(orderRequest.quantity())
                .build();
    }

    public static OrderDetails toDetails(Order order) {
        return OrderDetails.builder()
                .orderId(order.getOrderId())
                .externalOrderId(order.getExternalOrderId())
                .createdAt(order.getCreatedAt())
                .expirationDate(order.getExpirationDate())
                .userId(order.getUserId())
                .product(order.getProduct())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .updatedAt(order.getUpdatedAt())
                .alive(order.isAlive())
                .totalPrice(order.getTotalPrice())
                .build();
    }

    public static OrderEvent toEvent(OrderDetails order) {
        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrderId(order.getOrderId());
        orderEvent.setUserId(order.getUserId());
        orderEvent.setProduct(order.getProduct());
        orderEvent.setPrice(order.getPrice());
        orderEvent.setQuantity(order.getQuantity());
        orderEvent.setStatus(order.getStatus());
        orderEvent.setExpirationDate(order.getExpirationDate());
        orderEvent.setCreatedAt(order.getCreatedAt());
        orderEvent.setExternalOrderId(order.getExternalOrderId());
        orderEvent.setTotalPrice(order.getTotalPrice());
        orderEvent.setAlive(order.isAlive());
        orderEvent.setUpdatedAt(order.getUpdatedAt());
        return orderEvent;
    }
}

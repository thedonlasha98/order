package ge.croco.order.service;

import ge.croco.order.model.dto.OrderDetails;
import ge.croco.order.model.dto.OrderRequest;
import ge.croco.order.model.dto.UpdateOrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderDetails createOrder(Long userId, OrderRequest order);

    Page<OrderDetails> getOrders(Pageable pageable);

    OrderDetails getOrder(String orderId);

    OrderDetails updateOrder(String orderId, UpdateOrderRequest orderRequest);

    void deleteOrder(String orderId);

    void deleteByUserId(Long userId);
}

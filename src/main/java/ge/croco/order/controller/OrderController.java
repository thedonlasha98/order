package ge.croco.order.controller;

import ge.croco.order.model.dto.OrderDetails;
import ge.croco.order.model.dto.OrderRequest;
import ge.croco.order.model.dto.UpdateOrderRequest;
import ge.croco.order.security.JwtClaim;
import ge.croco.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetails createOrder(@JwtClaim Long userId,
                                    @RequestBody @Valid OrderRequest order) {
        return orderService.createOrder(userId, order);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<OrderDetails> getOrders(Pageable pageable) {
        return orderService.getOrders(pageable);
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public OrderDetails getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @PutMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public OrderDetails updateOrder(@PathVariable String orderId,
                                    @RequestBody @Valid UpdateOrderRequest orderRequest) {
        return orderService.updateOrder(orderId, orderRequest);
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
    }

}

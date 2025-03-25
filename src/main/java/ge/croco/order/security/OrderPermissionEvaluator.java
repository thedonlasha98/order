package ge.croco.order.security;

import ge.croco.order.domain.Order;
import ge.croco.order.model.dto.OrderDetails;
import ge.croco.order.service.OrderService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPermissionEvaluator {

    private final OrderService orderService;


    public boolean isOwner(Authentication authentication, String orderId) {
        Long authenticatedUserId = getUserIdFromAuth(authentication);

        OrderDetails order = orderService.getOrder(orderId);
        return order.getUserId().equals(authenticatedUserId);
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        return getClaims(authentication).get("userId", Long.class);
    }

    private Claims getClaims(Authentication authentication) {
        return (Claims) authentication.getCredentials();
    }
}
package ge.croco.order.repository;

import ge.croco.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    boolean existsByExternalOrderId(String externalOrderId);

    void deleteAllByUserId(Long userId);
}

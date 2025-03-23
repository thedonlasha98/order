package ge.croco.order.service;

import ge.croco.order.domain.Order;
import ge.croco.order.enums.EventType;
import ge.croco.order.enums.OrderStatus;
import ge.croco.order.exception.OrderAlreadyExistsException;
import ge.croco.order.exception.OrderInvalidStatusException;
import ge.croco.order.exception.OrderNotFoundException;
import ge.croco.order.model.dto.OrderDetails;
import ge.croco.order.model.event.OrderEvent;
import ge.croco.order.model.dto.OrderRequest;
import ge.croco.order.model.dto.UpdateOrderRequest;
import ge.croco.order.model.mapper.OrderMapper;
import ge.croco.order.repository.OrderRepository;
import ge.croco.order.util.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ge.croco.order.config.CacheConfig.ORDER_CACHE;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final OrderRepository orderRepository;

    private final CacheManager cacheManager;

    @Override
    public OrderDetails createOrder(Long userId, OrderRequest orderRequest) {
        if (orderRepository.existsByExternalOrderId(orderRequest.orderId())) {
            throw new OrderAlreadyExistsException(orderRequest.orderId());
        }

        Order order = OrderMapper.toEntity(orderRequest);
        order.setOrderId(UUID.randomUUID().toString());
        order.setExpirationDate(order.getCreatedAt().plusSeconds(orderRequest.ttl()));
        order.setTotalPrice(orderRequest.price().multiply(new BigDecimal(orderRequest.quantity())));
        order.setStatus(OrderStatus.PENDING);
        order.setAlive(true);

        orderRepository.save(order);
        OrderDetails orderDetails = OrderMapper.toDetails(order);
        sendOrderEvent(EventType.ORDER_CREATED, orderDetails);

        return orderDetails;
    }

    @Override
    public Page<OrderDetails> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderMapper::toDetails);
    }

    @Override
    @Cacheable(value = ORDER_CACHE, key = "#orderId")
    public OrderDetails getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(OrderMapper::toDetails)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    @CacheEvict(value = ORDER_CACHE, key = "#orderId")
    public OrderDetails updateOrder(String orderId, UpdateOrderRequest orderRequest) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!OrderValidator.isAliveStatus(order.getStatus())) {
            throw new OrderInvalidStatusException(order.getStatus());
        }

        if (OrderValidator.changedTotalPrice(orderRequest, order)) {
            if (!OrderValidator.isPossibleToChangeOrderDetails(order)) {
                throw new OrderInvalidStatusException(order.getStatus());
            }
            order.setQuantity(orderRequest.quantity());
            order.setPrice(orderRequest.price());
            order.setTotalPrice(order.getPrice().multiply(new BigDecimal(order.getQuantity())));
        }

        order.setStatus(orderRequest.status());
        order.setAlive(OrderValidator.isAliveStatus(order.getStatus()));

        orderRepository.save(order);
        OrderDetails orderDetails = OrderMapper.toDetails(order);
        sendOrderEvent(EventType.ORDER_UPDATED, orderDetails);

        return orderDetails;
    }

    @Override
    @CacheEvict(value = ORDER_CACHE, key = "#orderId")
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!OrderValidator.isValidToDeleteOrder(order)) {
            throw new OrderInvalidStatusException(order.getStatus());
        }

        order.setStatus(OrderStatus.DELETED);
        order.setAlive(OrderValidator.isAliveStatus(order.getStatus()));

        orderRepository.save(order);
        sendOrderEvent(EventType.ORDER_DELETED, OrderMapper.toDetails(order));

    }

    @Override
    public void deleteByUserId(Long userId) {
        orderRepository.deleteAllByUserId(userId);
        Optional.ofNullable(cacheManager.getCache(ORDER_CACHE))
                .ifPresent(Cache::clear);
    }

    private void sendOrderEvent(EventType eventType, OrderDetails order) {
        OrderEvent orderEvent = OrderMapper.toEvent(order);
        orderEvent.setEventType(eventType);
        orderEvent.setTimestamp(Instant.now());

        log.info("Sending order event: {}", orderEvent);
        try {
            kafkaTemplate.send("orders", eventType.name(), order).get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("Failed to send order event: {}", orderEvent, ex);
        }
    }
}

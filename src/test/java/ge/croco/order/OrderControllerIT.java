package ge.croco.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.croco.order.domain.Order;
import ge.croco.order.enums.EventType;
import ge.croco.order.enums.OrderStatus;
import ge.croco.order.model.dto.OrderDetails;
import ge.croco.order.model.dto.OrderRequest;
import ge.croco.order.model.dto.UpdateOrderRequest;
import ge.croco.order.model.event.OrderEvent;
import ge.croco.order.model.event.UserEvent;
import ge.croco.order.repository.OrderRepository;
import ge.croco.order.service.OrderService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static ge.croco.order.config.CacheConfig.ORDER_CACHE;
import static ge.croco.order.security.JwtTokenUtil.getSigningKey;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderControllerIT {
    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @Container
    private static final ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Container
    private static final GenericContainer<?> hazelcastContainer = new GenericContainer<>(DockerImageName.parse("hazelcast/hazelcast:5.3.5"))
            .withExposedPorts(5701);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("hazelcast.network.tcpip.enabled", () -> "true");
        registry.add("hazelcast.network.tcpip.members", () -> hazelcastContainer.getHost() + ":" + hazelcastContainer.getMappedPort(5701));

    }

    private static final String KAFKA_ORDER_TOPIC = "orders";

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String generateJwtToken(Long userId, String role) {
        return Jwts.builder()
                .claim("authorities", List.of("ROLE_" + role))
                .claim("userId", userId)
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 600000))
                .signWith(getSigningKey(secretKey), SignatureAlgorithm.HS512)
                .compact();
    }

    private static ConsumerRecord<String, String> getLatestRecordForTopic(Consumer<String, String> consumer, String topic) {
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1));
        AtomicReference<ConsumerRecord<String, String>> record = new AtomicReference<>();
        records.records(topic).iterator().forEachRemaining(record::set);
        return record.get();
    }

    private static @NotNull Consumer<String, String> registerKafkaConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "testGroup", "true");
        consumerProps.put("auto.offset.reset", "earliest");
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer()
        );

        return cf.createConsumer();
    }

    @BeforeEach
    void setUp() {
        System.out.println("Before each run");
        cacheManager.getCache(ORDER_CACHE).clear();
        jdbcTemplate.execute("TRUNCATE TABLE orders RESTART IDENTITY;");
    }

    @Test
    public void createOrder_success() throws Exception {
        OrderRequest orderRequest = new OrderRequest(
                UUID.randomUUID().toString(),
                "Macbook",
                3,
                new BigDecimal(10000),
                1800
        );

        Consumer<String, String> consumer = registerKafkaConsumer();
        consumer.subscribe(Collections.singletonList(KAFKA_ORDER_TOPIC));

        Long userId = 1L;
        String token = generateJwtToken(userId, "USER");

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(orderRequest))
        );

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderId").value(orderRequest.orderId()))
                .andExpect(jsonPath("$.product").value(orderRequest.product()))
                .andExpect(jsonPath("$.quantity").value(orderRequest.quantity()))
                .andExpect(jsonPath("$.price").value(orderRequest.price()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name()))
                .andExpect(jsonPath("$.totalPrice").value(orderRequest.price().multiply(new BigDecimal(orderRequest.quantity()))))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.alive").value(true));

        ConsumerRecord<String, String> record = getLatestRecordForTopic(consumer, KAFKA_ORDER_TOPIC);
        consumer.close();

        OrderEvent orderEvent = objectMapper.readValue(record.value(), OrderEvent.class);
        System.out.println(orderEvent);
        Assertions.assertEquals(EventType.ORDER_CREATED, orderEvent.getEventType());
        Assertions.assertEquals(OrderStatus.PENDING, orderEvent.getStatus());
        Assertions.assertEquals(userId, orderEvent.getUserId());
        Assertions.assertEquals(orderRequest.product(), orderEvent.getProduct());
        Assertions.assertEquals(orderRequest.quantity(), orderEvent.getQuantity());
        Assertions.assertEquals(orderRequest.price(), orderEvent.getPrice());
        Assertions.assertEquals(orderRequest.orderId(), orderEvent.getExternalOrderId());

        System.out.println("createOrder_success passed successfully!");
    }

    @Test
    public void getOrderByOwner_returnOrder_forOther_FORBIDDEN() throws Exception {
        OrderRequest orderRequest = new OrderRequest(
                UUID.randomUUID().toString(),
                "Macbook",
                3,
                new BigDecimal(10000),
                1800
        );

        Long userId = 1L;
        String token = generateJwtToken(userId, "USER");

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(orderRequest))
        ).andExpect(status().isCreated()).andReturn();

        OrderDetails createdOrder = objectMapper.readValue(createResult.getResponse().getContentAsByteArray(), OrderDetails.class);

        MvcResult getOwnerResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/" + createdOrder.getOrderId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(orderRequest))
        ).andExpect(status().isOk()).andReturn();

        OrderDetails getOrder = objectMapper.readValue(getOwnerResult.getResponse().getContentAsByteArray(), OrderDetails.class);
        Assertions.assertEquals(createdOrder.getExternalOrderId(), getOrder.getExternalOrderId());

        OrderDetails cachedOrder = cacheManager.getCache(ORDER_CACHE).get(getOrder.getOrderId(), OrderDetails.class);
        Assertions.assertEquals(getOrder.getExternalOrderId(), cachedOrder.getExternalOrderId());

        String otherToken = generateJwtToken(2L, "USER");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/" + createdOrder.getOrderId())
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(orderRequest))
        ).andExpect(status().isForbidden());

        System.out.println("getOrderByOwner_returnOrder_forOther_FORBIDDEN passed successfully!");
    }

    @Test
    public void updateOrderByADMIN_changedStatusToCONFIRMED() throws Exception {
        OrderRequest orderRequest = new OrderRequest(
                UUID.randomUUID().toString(),
                "Macbook",
                3,
                new BigDecimal(10000),
                1800
        );

        Long userId = 1L;
        String token = generateJwtToken(userId, "USER");

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(orderRequest))
        ).andExpect(status().isCreated()).andReturn();

        OrderDetails createdOrder = objectMapper.readValue(createResult.getResponse().getContentAsByteArray(), OrderDetails.class);

        String adminToken = generateJwtToken(2L, "ADMIN");

        UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest(
                orderRequest.product(),
                createdOrder.getQuantity() + 1,
                createdOrder.getPrice(),
                OrderStatus.CONFIRMED
        );

        MvcResult updatedOrderResult = mockMvc.perform(MockMvcRequestBuilders.put("/api/orders/" + createdOrder.getOrderId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(updateOrderRequest))
        ).andExpect(status().isOk()).andReturn();

        OrderDetails updatedCache = cacheManager.getCache(ORDER_CACHE).get(createdOrder.getOrderId(), OrderDetails.class);
        Assertions.assertNull(updatedCache);

        MvcResult getOrderResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/" + createdOrder.getOrderId())
                .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk()).andReturn();

        OrderDetails getOrder = objectMapper.readValue(getOrderResult.getResponse().getContentAsByteArray(), OrderDetails.class);

        Assertions.assertEquals(updateOrderRequest.status(), getOrder.getStatus());
        Assertions.assertTrue(getOrder.isAlive());
        assertThat(getOrder.getTotalPrice()).isEqualByComparingTo(
                updateOrderRequest.price().multiply(new BigDecimal(updateOrderRequest.quantity()))
        );

        OrderDetails getCache = cacheManager.getCache(ORDER_CACHE).get(createdOrder.getOrderId(), OrderDetails.class);
        Assertions.assertEquals(updateOrderRequest.status(), getCache.getStatus());
        Assertions.assertTrue(getCache.isAlive());
        System.out.println("getOrderByOwner_returnOrder_forOther_FORBIDDEN passed successfully!");
    }

    @Test
    public void receiveUserDeletedEvent_shouldDeleteAllUserOrders() throws ExecutionException, InterruptedException, TimeoutException {
        Long userId = 1L;
        OrderRequest orderRequest = new OrderRequest(
                UUID.randomUUID().toString(),
                "Macbook",
                3,
                new BigDecimal(10000),
                1800
        );

        OrderRequest orderRequest2 = new OrderRequest(
                UUID.randomUUID().toString(),
                "Macbook",
                3,
                new BigDecimal(10000),
                1800
        );

        OrderRequest orderRequest3 = new OrderRequest(
                UUID.randomUUID().toString(),
                "Macbook",
                3,
                new BigDecimal(10000),
                1800
        );

        OrderDetails order = orderService.createOrder(userId, orderRequest);
        OrderDetails order2 = orderService.createOrder(userId, orderRequest2);
        OrderDetails order3 = orderService.createOrder(3L, orderRequest3);

        orderService.getOrder(order.getOrderId());
        orderService.getOrder(order3.getOrderId());

        OrderDetails cachedOrder = cacheManager.getCache(ORDER_CACHE).get(order.getOrderId(), OrderDetails.class);
        OrderDetails cachedOrder3 = cacheManager.getCache(ORDER_CACHE).get(order3.getOrderId(), OrderDetails.class);
        Assertions.assertNotNull(cachedOrder);
        Assertions.assertNotNull(cachedOrder3);

        UserEvent userEvent = new UserEvent("USER_DELETED", Instant.now());
        userEvent.setId(userId);
        kafkaTemplate.send("users", userId.toString(), userEvent).get(500, TimeUnit.MILLISECONDS);

        Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer orderCountAfter = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId
            );
            assertThat(orderCountAfter).isEqualTo(0);
        });
        List<Order> orders = orderRepository.findAll();
        Assertions.assertEquals(1, orders.size());
        boolean existsOrders = orders.stream()
                .anyMatch(o -> o.getUserId().equals(userId));
        Assertions.assertFalse(existsOrders);

        assertThat(cacheManager.getCache(ORDER_CACHE).get(order3.getOrderId())).isNull();
        assertThat(cacheManager.getCache(ORDER_CACHE).get(order.getOrderId())).isNull();


    }
}

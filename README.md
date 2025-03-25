# 📌 Order Management Service - Spring Boot, Kafka, Testcontainers

## 📖 Overview

This is a **Spring Boot microservice** that provides order management features. It integrates with **Kafka for event-driven messaging**, uses **Testcontainers for database integration testing**, and is containerized with **Docker & Docker Compose**.

## 🚀 Features

- **Spring Boot 3.x**
- **Kafka Integration** for messaging
- **Testcontainers** for PostgreSQL and Kafka testing
- **Gradle Build System**
- **Docker & Docker Compose**
- **Spring Data JPA** with PostgreSQL
- **Spring Security**
- **Hazelcast** for caching

---

## 📦 Project Structure

```
user-service/
│── src/main/java/ge/croco/order/  # Main source code
│── src/test/java/ge/croco/order/  # Unit & Integration tests
│── src/main/resources/
│── Dockerfile  # Containerization
│── docker-compose.yaml  # Running with Docker
│── build.gradle  # Gradle dependencies
│── README.md  # Documentation
```

---

## ⚙️ Setup & Installation

### Prerequisites
- **Java 21+**
- **Gradle**
- **Docker & Docker Compose**

### Quick Start
1. Clone the repository
2. Run the application with `script.sh`(Unix Based) or `script.bat`(Windows)

---

### Shut Down
Run with `shutdown.sh` (Unix Based)  or `shutdown.bat` (Windows)

---

## To start Kafka:
It will be start in Quick Start script
```sh
docker-compose -f docker-compose.kafka.yaml up -d
```

---

## 🧪 Build

- **Build with**: `./gradlew clean build -x test`

## 🧪 Running Tests
- **Unit and Integration Tests**: `./gradlew test`
---

## 📜 API Endpoints
| Method   | Endpoint           | Description             |
|----------|--------------------|-------------------------|
| `POST`   | `/api/orders`       | Create a new order                            |
| `GET`    | `/api/orders`       | Get all orders (Admin only)                   |
| `GET`    | `/api/orders/{id}`  | Get order details (Admin or order owner only) |
| `PUT`    | `/api/orders/{id}`  | Update order (Admin or order owner only)      |
| `DELETE` | `/api/orders/{id}`  | Delete order (Admin or order owner only)      |


---

## 🤝 Contributing
1. Fork the repo
2. Create a feature branch
3. Commit changes & push
4. Open a PR

---

#!/bin/bash

APP_CONTAINER="order-service"

# Function to check if a container exists
container_exists() {
    docker ps -a --format "{{.Names}}" | grep -w "$1" > /dev/null 2>&1
}

# Function to check if a container is running
container_running() {
    docker ps --format "{{.Names}}" | grep -w "$1" > /dev/null 2>&1
}

# Function to check if a network exists
network_exists() {
    docker network ls --format "{{.Name}}" | grep -w "$1" > /dev/null 2>&1
}

# Step 2: Build the Spring Boot applications using Gradle
echo "🔨 Building Spring Boot applications..."
./gradlew clean build -x test

# Step 3: Check if Kafka is already running
KAFKA_CONTAINER="kafka"

if container_running "$KAFKA_CONTAINER"; then
    echo "✅ Kafka is already running. Skipping Kafka setup."
else
    echo "🐳 Kafka not running. Building and starting Kafka..."
    docker-compose -f docker-compose.kafka.yaml build
    docker-compose -f docker-compose.kafka.yaml up -d
fi

# Step 4: Build Docker images for the applications
echo "📦 Building Docker images for applications..."
docker-compose -f docker-compose.yaml build

# Step 1: Ensure `app-network` exists
NETWORK_NAME="app-network"

if network_exists "$NETWORK_NAME"; then
    echo "✅ Docker network $NETWORK_NAME already exists."
    docker network connect $NETWORK_NAME $APP_CONTAINER
    docker network connect $NETWORK_NAME $KAFKA_CONTAINER


else
    echo "🌐 Creating Docker network: $NETWORK_NAME..."
    docker network create "$NETWORK_NAME"
    docker network connect $NETWORK_NAME $APP_CONTAINER
    docker network connect $NETWORK_NAME $KAFKA_CONTAINER

fi

# Step 5: Start Docker containers for applications, databases, and Hazelcast
echo "🚀 Starting application containers..."
docker-compose -f docker-compose.yaml up -d

# Step 6: Optional: Initialize database schema if needed
#echo "Initializing database schema and seeding data..."
#docker exec -it postgres_container psql -U your_db_user -d your_db_name -f /docker-entrypoint-initdb.d/init.sql

echo "✅ Setup complete!"

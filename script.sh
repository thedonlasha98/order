#!/bin/bash

# Step 1: Build the Spring Boot applications using Gradle
echo "Building Spring Boot applications..."
./gradlew clean build -x test

# Step 2: Build Docker images for Kafka and Zookeeper
echo "Building Docker images for Kafka and Zookeeper..."
docker-compose -f docker-compose.kafka.yaml build

# Step 3: Build Docker images for the applications
echo "Building Docker images for applications..."
docker-compose -f docker-compose.yaml build

# Step 4: Start Docker containers for applications, databases, Hazelcast, and Kafka
echo "Starting Docker containers..."
docker-compose -f docker-compose.kafka.yaml -f docker-compose.yaml  up -d

## Step 5: Initialize the database schema and seed data
#echo "Initializing database schema and seeding data..."
#docker exec -it postgres_container psql -U your_db_user -d your_db_name -f /docker-entrypoint-initdb.d/init.sql

echo "Setup complete!"
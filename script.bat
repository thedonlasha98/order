@echo off

REM Step 1: Build the Spring Boot applications using Gradle
echo Building Spring Boot applications...
gradlew.bat clean build -x test


REM Step 2: Build Docker images for Kafka and Zookeeper
echo Building Docker images for Kafka and Zookeeper...
docker-compose -f docker-compose.kafka.yaml build

REM Step 3: Build Docker images for the applications
echo Building Docker images for applications...
docker-compose -f docker-compose.yaml build

REM Step 4: Start Docker containers for applications, databases, Hazelcast, and Kafka
echo Starting Docker containers...
docker-compose -f docker-compose.kafka.yaml -f docker-compose.yaml up -d

@REM REM Step 5: Initialize the database schema and seed data
@REM echo Initializing database schema and seeding data...
@REM docker exec -it postgres_container psql -U your_db_user -d your_db_name -f C:\path\to\init.sql

echo Setup complete!
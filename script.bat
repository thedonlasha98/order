@echo off
SET APP_CONTAINER=order-service
SET KAFKA_CONTAINER=kafka
SET NETWORK_NAME=app-network

echo 🔨 Building Spring Boot applications...
call gradlew clean build -x test

:: Check if Kafka is running
for /f %%i in ('docker ps --format "{{.Names}}" ^| findstr /C:"%KAFKA_CONTAINER%"') do set FOUND_KAFKA=%%i

if defined FOUND_KAFKA (
    echo ✅ Kafka is already running. Skipping Kafka setup.
) else (
    echo 🐳 Kafka not running. Building and starting Kafka...
    docker-compose -f docker-compose.kafka.yaml build
    docker-compose -f docker-compose.kafka.yaml up -d
)

:: Build Docker images for applications
echo 📦 Building Docker images for applications...
docker-compose -f docker-compose.yaml build

:: Check if the network exists
for /f %%i in ('docker network ls --format "{{.Name}}" ^| findstr /C:"%NETWORK_NAME%"') do set FOUND_NETWORK=%%i

if defined FOUND_NETWORK (
    echo ✅ Docker network %NETWORK_NAME% already exists.
) else (
    echo 🌐 Creating Docker network: %NETWORK_NAME%...
    docker network create %NETWORK_NAME%
)

:: Connect containers to the network
docker network connect %NETWORK_NAME% %APP_CONTAINER% 2>nul
docker network connect %NETWORK_NAME% %KAFKA_CONTAINER% 2>nul

:: Start application containers
echo 🚀 Starting application containers...
docker-compose -f docker-compose.yaml up -d

echo ✅ Setup complete!

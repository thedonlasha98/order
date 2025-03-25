@echo off

REM Step 1: Stop Docker containers for applications, databases, Hazelcast, and Kafka
echo Stopping Docker containers...
docker-compose -f docker-compose.yaml -f docker-compose.kafka.yaml down

echo All containers stopped!
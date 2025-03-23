# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jdk

# Set the working directory
WORKDIR /app

# Copy the application JAR file
COPY build/libs/order-0.0.1.jar app.jar

# Expose the application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
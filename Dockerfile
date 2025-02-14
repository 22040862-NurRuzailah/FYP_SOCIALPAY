# Use OpenJDK 17 as the base image for the container
FROM openjdk:17-jdk-slim

# Set the working directory to /app
WORKDIR /app

# Copy the Spring Boot jar file from the target directory into the container
COPY target/FYP-1-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose the port that the app will run on (default Spring Boot port is 8080)
EXPOSE 8080

# Command to run the app
CMD ["java", "-jar", "app.jar"]

# Use an official OpenJDK image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the .jar file into the container
COPY target/FYP-1-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose the port that your Spring Boot app will run on (default is 8080)
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]

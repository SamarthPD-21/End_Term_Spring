#!/bin/bash
# Run GitUpskill Backend with Java 21

export JAVA_HOME=/home/samarth/.local/share/mise/installs/java/21.0.2
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using Java: $(java -version 2>&1 | head -1)"
echo "Starting GitUpskill Backend..."

# Run Spring Boot
./mvnw spring-boot:run -DskipTests

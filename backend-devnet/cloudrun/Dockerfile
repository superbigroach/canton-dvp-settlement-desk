# Minimal runtime image for Cloud Run — runs the prebuilt CrossDesk devnet jar.
# Cloud Run injects $PORT; Spring listens on it via -Dserver.port.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY app.jar /app/app.jar
ENV PORT=8080
ENTRYPOINT ["sh","-c","java -Dserver.port=${PORT} -jar /app/app.jar"]

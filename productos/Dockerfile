# 1. Etapa de compilación
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Etapa de ejecución
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# --- AQUÍ ESTÁ LA CORRECCIÓN CLAVE ---
# Usamos el nombre exacto que sale de tu pom.xml
COPY --from=build /app/target/productos-0.0.1-SNAPSHOT.jar app.jar
# -------------------------------------

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
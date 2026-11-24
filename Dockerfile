# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY glowy/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY glowy/src ./src
RUN mvn -q -B clean package -DskipTests

# ---------- Etapa 2: executar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Limita a memória da JVM (o plano gratuito do Render tem 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Xss512k"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
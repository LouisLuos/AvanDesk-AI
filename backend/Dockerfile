# =======================================================
# Estágio 1: Build com Maven e JDK 21
# =======================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copia descritor de dependências para aproveitar cache do Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copia código-fonte e compila gerando o .jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# =======================================================
# Estágio 2: Imagem final enxuta com JRE 21 (Alpine Linux)
# =======================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia apenas o .jar gerado no primeiro estágio
COPY --from=builder /app/target/api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Otimização de memória para caber perfeitamente nos 512MB do Render Free:
# - SerialGC: coletor ultraleve com menor pegada de memória
# - Xmx384m: limite máximo de Heap seguro para não estourar os 512MB da máquina
ENV JAVA_OPTS="-XX:+UseSerialGC -Xmx384m -Xms128m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]


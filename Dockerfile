# Multi-stage build para aplicación Spring Boot
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Configurar Maven para builds más rápidos
ENV MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 -Dmaven.wagon.http.retryHandler.count=3"

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Descargar dependencias con timeouts más largos (para aprovechar cache de Docker)
RUN mvn dependency:go-offline -B \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    -Dmaven.wagon.httpconnectionManager.maxTotal=30 \
    -Dmaven.wagon.httpconnectionManager.maxPerRoute=10

# Copiar código fuente
COPY src ./src

# Compilar la aplicación con configuraciones optimizadas
RUN mvn clean package -DskipTests -B \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=25

# Etapa de runtime
FROM eclipse-temurin:17-jre

# Instalar curl para healthcheck
RUN apt update && apt install -y curl && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root para seguridad
RUN groupadd -g 1001 booky && \
    useradd -r -u 1001 -g booky booky

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar ownership del archivo
RUN chown booky:booky app.jar

# Cambiar a usuario no-root
USER booky

# Exponer puerto
EXPOSE 8080

# Healthcheck comentado temporalmente - requiere Spring Boot Actuator
# HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#   CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"] 
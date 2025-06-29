# Multi-stage build para aplicación Spring Boot
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Descargar dependencias (para aprovechar cache de Docker)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

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
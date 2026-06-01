# Tahap 1: Builder
FROM eclipse-temurin:21-jdk-alpine AS builder 
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Tahap 2: Running Environment
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# 1. Install alat tempur untuk script restore kita
RUN apk add --no-cache bash curl

# 2. Ambil file .jar hasil dari tahap builder
COPY --from=builder /app/target/*.jar app.jar

# 3. Masukkan script bash kita dan beri izin eksekusi
COPY render-start.sh .
RUN chmod +x render-start.sh

# 4. Siapkan folder database
RUN mkdir -p data && chmod 777 data

# 5. Port wajib Hugging Face
EXPOSE 7860

# 6. Eksekusi script restore sebelum Java menyala
CMD ["bash", "render-start.sh"]
# 빌드 스테이지 — Maven 의존성 캐시를 위해 pom.xml만 먼저 복사해서 go-offline 실행
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# 실행 스테이지 — JDK 대신 JRE만 사용해서 이미지 크기 축소
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

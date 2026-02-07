FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target /app/target

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["sh", "-c", "java -jar $(find /app/target -maxdepth 1 -name '*.jar' ! -name '*original*' | head -n 1)"]

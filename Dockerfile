# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8081
# 기본은 상주 + 내부 스케줄러. ECS Scheduled Task로 쓸 땐
# command override 로 --batch.job=rankingJob 같은 인자를 넘기면 해당 Job만 실행 후 종료한다.
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]

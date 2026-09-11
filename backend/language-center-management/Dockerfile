FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre-ubi10-minimal AS runtime

WORKDIR /app

# InvoicePdfService uses this Unicode font when no custom font path is configured.
RUN microdnf install -y dejavu-sans-fonts \
    && microdnf clean all

COPY --from=build --chown=1001:0 \
    /workspace/target/language-center-management-*.jar /app/app.jar

USER 1001

ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Stage 1: build the WAR with Maven (tests are skipped here: they need a running server)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Stage 2: run it on WildFly, deployed as ROOT.war so the API is at /api
FROM quay.io/wildfly/wildfly:41.0.1.Final-jdk21
COPY --from=build /app/target/library.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
EXPOSE 8080
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]

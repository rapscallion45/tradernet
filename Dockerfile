FROM maven:3.9.6-eclipse-temurin-11 AS build

WORKDIR /workspace/tradernet

COPY pom.xml pom.xml
COPY backend/pom.xml backend/pom.xml
COPY backend/api/pom.xml backend/api/pom.xml
COPY backend/data-model/pom.xml backend/data-model/pom.xml
COPY backend/services/pom.xml backend/services/pom.xml
COPY frontend/pom.xml frontend/pom.xml
COPY frontend/package.json frontend/yarn.lock frontend/

COPY . .

RUN mvn -B -DskipTests clean package

FROM quay.io/wildfly/wildfly:27.0.1.Final

COPY --from=build /workspace/tradernet/backend/api/target/tradernet.war /opt/jboss/wildfly/standalone/deployments/tradernet.war

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]

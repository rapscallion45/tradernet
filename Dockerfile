FROM maven:3.9.6-eclipse-temurin-11 AS build

WORKDIR /workspace/tradernet

COPY pom.xml pom.xml
COPY api/pom.xml api/pom.xml
COPY data-model/pom.xml data-model/pom.xml
COPY services/facade-service/pom.xml services/facade-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/signal-service/pom.xml services/signal-service/pom.xml
COPY services/trade-service/pom.xml services/trade-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY web/pom.xml web/pom.xml
COPY web/package.json web/yarn.lock web/

COPY . .

RUN mvn -B -DskipTests clean package

FROM quay.io/wildfly/wildfly:27.0.1.Final

COPY --from=build /workspace/tradernet/api/target/tradernet.war /opt/jboss/wildfly/standalone/deployments/tradernet.war

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]

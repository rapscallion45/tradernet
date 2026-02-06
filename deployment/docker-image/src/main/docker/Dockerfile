FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk11

COPY ROOT.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
COPY tradernet.war /opt/jboss/wildfly/standalone/deployments/tradernet.war
COPY data-model.jar /opt/jboss/wildfly/standalone/deployments/data-model.jar
COPY order-service.jar /opt/jboss/wildfly/standalone/deployments/order-service.jar
COPY trade-service.jar /opt/jboss/wildfly/standalone/deployments/trade-service.jar
COPY user-service.jar /opt/jboss/wildfly/standalone/deployments/user-service.jar
COPY signal-service.jar /opt/jboss/wildfly/standalone/deployments/signal-service.jar
COPY facade-service.jar /opt/jboss/wildfly/standalone/deployments/facade-service.jar
RUN touch /opt/jboss/wildfly/standalone/deployments/ROOT.war.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/tradernet.war.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/data-model.jar.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/order-service.jar.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/trade-service.jar.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/user-service.jar.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/signal-service.jar.dodeploy \
    && touch /opt/jboss/wildfly/standalone/deployments/facade-service.jar.dodeploy \
    && rm -rf /opt/jboss/wildfly/welcome-content
COPY entryscript.sh /bin/entryscript.sh
COPY wildfly-modules/org/postgresql/main/module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml
COPY postgresql.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar
COPY wildfly-modules/com/tradernet/data-model/main/module.xml /opt/jboss/wildfly/modules/com/tradernet/data-model/main/module.xml
COPY data-model.jar /opt/jboss/wildfly/modules/com/tradernet/data-model/main/data-model.jar
COPY wildfly-modules/org/springframework/security/crypto/main/module.xml /opt/jboss/wildfly/modules/org/springframework/security/crypto/main/module.xml
COPY spring-security-crypto.jar /opt/jboss/wildfly/modules/org/springframework/security/crypto/main/spring-security-crypto.jar

USER root
RUN sed -i 's/\r$//' /bin/entryscript.sh \
    && chmod +x /bin/entryscript.sh
USER jboss

LABEL org.opencontainers.image.title="Tradernet"
LABEL org.opencontainers.image.version="${project.version}"

ENV DB_TYPE=POSTGRES \
    DB_HOST=localhost \
    DB_PORT=5432 \
    DB_NAME=tradernet \
    DB_USER=tradernet \
    DB_PASSWORD=tradernet

EXPOSE 8080 9990

ENTRYPOINT ["/bin/entryscript.sh"]

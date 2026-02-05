FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk11

COPY tradernet.war /opt/jboss/wildfly/standalone/deployments/tradernet.war
COPY entryscript.sh /bin/entryscript.sh
COPY wildfly-modules/org/postgresql/main/module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml
COPY postgresql.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar

USER root
RUN chmod +x /bin/entryscript.sh
USER jboss

LABEL org.opencontainers.image.title="Tradernet"
LABEL org.opencontainers.image.version="${project.version}"

ENV DB_TYPE=POSTGRES \
    DB_HOST=localhost \
    DB_PORT=5432 \
    DB_NAME=tradernet \
    DB_USER=tradernet \
    DB_PASSWORD=tradernet

EXPOSE 8080

ENTRYPOINT ["/bin/entryscript.sh"]

FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk11

COPY tradernet.ear /opt/jboss/wildfly/standalone/deployments/tradernet.ear
RUN rm -rf /opt/jboss/wildfly/welcome-content
COPY entryscript.sh /bin/entryscript.sh
COPY wildfly-modules.zip /opt/jboss/wildfly/wildfly-modules.zip
RUN unzip -q /opt/jboss/wildfly/wildfly-modules.zip -d /opt/jboss/wildfly/modules \
    && rm -f /opt/jboss/wildfly/wildfly-modules.zip
COPY postgresql.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar

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

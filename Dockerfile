FROM openjdk:11-slim

ARG JAR_FILE=census-rm-fieldwork-adapter*.jar
COPY target/$JAR_FILE /opt/census-rm-fieldwork-adapter.jar

COPY healthcheck.sh /opt/healthcheck.sh
RUN chmod +x /opt/healthcheck.sh

CMD exec /usr/local/openjdk-11/bin/java $JAVA_OPTS -jar /opt/census-rm-fieldwork-adapter.jar

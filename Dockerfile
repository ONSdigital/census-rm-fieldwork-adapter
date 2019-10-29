FROM openjdk:11-jdk-slim

ARG JAR_FILE=census-rm-fieldwork-adapter*.jar
CMD ["/usr/local/openjdk-11/bin/java", "-jar", "/opt/census-rm-fieldwork-adapter.jar"]

COPY healthcheck.sh /opt/healthcheck.sh
RUN chmod +x /opt/healthcheck.sh

RUN groupadd --gid 999 fieldworkadapter && \
    useradd --create-home --system --uid 999 --gid fieldworkadapter fieldworkadapter
USER fieldworkadapter

COPY target/$JAR_FILE /opt/census-rm-fieldwork-adapter.jar

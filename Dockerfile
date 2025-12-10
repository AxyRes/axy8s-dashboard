FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

WORKDIR /opt/app

COPY target/*.jar axy8s.jar

USER root
RUN useradd -r -u 1001 axyuser && \
    chown -R axyuser:axyuser /opt/app

USER axyuser

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/app/axy8s.jar"]

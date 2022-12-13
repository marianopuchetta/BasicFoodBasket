FROM amazoncorretto:11-alpine-jdk
MAINTAINER mpuchetta
COPY target/backend.jar backend.jar
ENTRYPOINT ["java","-jar","/backend.jar"]
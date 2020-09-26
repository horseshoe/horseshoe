
ARG JRE_IMAGE=adoptopenjdk/openjdk11:alpine-jre

FROM ubuntu AS build

RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends default-jdk git && \
	rm -rf /var/lib/apt/lists/*

COPY . /usr/src/horseshoe
WORKDIR /usr/src/horseshoe

RUN ./gradlew clean jar


FROM ${JRE_IMAGE} AS deploy

COPY --from=build /usr/src/horseshoe/build/libs/horseshoe-*.jar /usr/bin/
RUN cd /usr/bin && ln -s horseshoe-*.jar horseshoe.jar
WORKDIR /root

ENTRYPOINT ["java", "-jar", "/usr/bin/horseshoe.jar"]
CMD ["--version"]

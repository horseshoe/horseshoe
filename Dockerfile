
ARG JRE_IMAGE=adoptopenjdk/openjdk11:jre
ARG JRE_IMAGE_X64=adoptopenjdk/openjdk11:alpine-jre
ARG DEPLOY=deploy-built-jar

# Build using Ubuntu JDK
FROM ubuntu AS build

RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends default-jdk-headless git && \
	rm -rf /var/lib/apt/lists/*

COPY . /usr/src/horseshoe
WORKDIR /usr/src/horseshoe

RUN ./gradlew clean jar

# Deploy using JRE
FROM ${JRE_IMAGE} AS jre-linux-default
FROM ${JRE_IMAGE_X64} AS jre-linux-amd64
FROM ${JRE_IMAGE} AS jre-linux-arm
FROM ${JRE_IMAGE} AS jre-linux-arm64
FROM ${JRE_IMAGE} AS jre-linux-ppc64le
FROM ${JRE_IMAGE} AS jre-linux-s390x

FROM jre-${TARGETOS:-linux}-${TARGETARCH:-default} AS jre-base

# Deploy from either a local JAR or the built JAR
FROM jre-base AS deploy-built-jar
ONBUILD COPY --from=build /usr/src/horseshoe/build/libs/horseshoe-*.jar /usr/bin/

FROM jre-base AS deploy-local-jar
ONBUILD ARG JAR_FILE=/usr/src/horseshoe/build/libs/horseshoe-*.jar
ONBUILD COPY ${JAR_FILE} /usr/bin/

FROM ${DEPLOY} AS deploy

RUN cd /usr/bin && ln -s horseshoe-*.jar horseshoe.jar
WORKDIR /root

ENTRYPOINT ["java", "-jar", "/usr/bin/horseshoe.jar"]
CMD ["--version"]

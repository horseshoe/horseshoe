
ARG JRE_IMAGE=adoptopenjdk/openjdk11:jre
ARG JRE_IMAGE_X64=adoptopenjdk/openjdk11:alpine-jre

# Build using Ubuntu JDK
FROM ubuntu AS build

RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends default-jdk-headless git && \
	rm -rf /var/lib/apt/lists/*

COPY . /usr/src/horseshoe
WORKDIR /usr/src/horseshoe

RUN ./gradlew clean jar

# Deploy using JRE
FROM ${JRE_IMAGE} AS jre-default
FROM ${JRE_IMAGE_X64} AS jre-amd64
FROM ${JRE_IMAGE} AS jre-arm
FROM ${JRE_IMAGE} AS jre-arm64
FROM ${JRE_IMAGE} AS jre-ppc64le
FROM ${JRE_IMAGE} AS jre-s390x

FROM jre-${TARGETARCH:-default} AS jre-base

# Deploy from either a local JAR or the built JAR
FROM jre-base AS deploy-built-jar
COPY --from=build /usr/src/horseshoe/build/libs/*.jar /usr/lib/horseshoe.jar

FROM jre-base AS deploy-local-jar
ONBUILD ARG JAR_FILE=build/libs/*.jar
ONBUILD COPY ${JAR_FILE} /usr/lib/

FROM deploy-${DEPLOY:-built-jar} AS deploy

RUN addgroup horseshoe && adduser --gecos Horseshoe --shell /sbin/nologin --ingroup horseshoe --disabled-password --no-create-home horseshoe
USER horseshoe

RUN java -jar /usr/lib/horseshoe.jar --version
ENTRYPOINT ["java", "-jar", "/usr/lib/horseshoe.jar"]
CMD ["--version"]

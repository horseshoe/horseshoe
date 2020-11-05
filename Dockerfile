
ARG JDK_IMAGE=adoptopenjdk:11
ARG DEPLOY=built-jar

# Build using Ubuntu JDK
FROM ubuntu AS build

RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends default-jdk-headless git && \
	rm -rf /var/lib/apt/lists/*

COPY . /usr/src/horseshoe
WORKDIR /usr/src/horseshoe
RUN ./gradlew clean jar

# Build a minimal image from the alpine JDK
FROM alpine AS jdk-alpine
RUN addgroup horseshoe && adduser --gecos Horseshoe --shell /sbin/nologin --ingroup horseshoe --disabled-password --no-create-home horseshoe
RUN apk add binutils openjdk11

RUN /usr/lib/jvm/default-jvm/bin/jlink --compress=2 --add-modules  java.base,java.logging --no-header-files --no-man-pages --strip-debug --output /opt/jre
RUN rm -rf /opt/jre/bin/keytool
RUN find /opt/jre -name '*.so' | xargs strip
RUN find /lib -name '*.so*' | xargs strip

# Create a scratch alpine JRE
FROM scratch AS jre-scratch-alpine

COPY --from=jdk-alpine /etc/passwd /etc/group /etc/
COPY --from=jdk-alpine /lib/ld-*.so* /lib/libc.*.so* /lib/libz.so* /lib/
COPY --from=jdk-alpine /opt/jre/ /usr/
ENV LD_LIBRARY_PATH=/lib:/usr/lib:/usr/lib/jli

# Build a minimal image from a small Debian JRE base image
FROM ${JDK_IMAGE} AS jdk-default
RUN addgroup horseshoe && adduser --gecos Horseshoe --shell /sbin/nologin --ingroup horseshoe --disabled-password --no-create-home horseshoe

RUN apt-get update && \
	apt-get install -y --no-install-recommends binutils && \
	rm -rf /var/lib/apt/lists/*

RUN jlink --compress=2 --add-modules  java.base,java.logging --no-header-files --no-man-pages --strip-debug --output /opt/jre
RUN rm -rf /opt/jre/bin/keytool
RUN find /opt/jre -name '*.so' | xargs strip
RUN find /usr/lib -name '*.so*' | xargs strip

# Create a scratch JRE
FROM scratch AS jre-scratch

COPY --from=jdk-default /etc/passwd /etc/group /etc/
COPY --from=jdk-default /usr/lib/*-linux-*/ld-* /usr/lib/*-linux-*/lib[cmz][\\-.]* /usr/lib/*-linux-*/libdl[\\-.]* /usr/lib/*-linux-*/libpthread[\\-.]* /usr/lib/*-linux-*/librt[\\-.]* /lib/
COPY --from=jdk-default /usr/lib/*-linux-*/ld-* /lib64/
COPY --from=jdk-default /opt/jre/ /usr/
ENV LD_LIBRARY_PATH=/lib:/usr/lib:/usr/lib/jli

# Only some architectures support openjdk11 on alpine
FROM jre-scratch-alpine AS jre-amd64
FROM jre-scratch AS jre-arm
FROM jre-scratch-alpine AS jre-arm64
FROM jre-scratch-alpine AS jre-ppc64le
FROM jre-scratch-alpine AS jre-s390x

FROM jre-${TARGETARCH:-scratch} AS jre-base

# Deploy from either a local JAR or the built JAR
FROM jre-base AS deploy-built-jar
COPY --from=build /usr/src/horseshoe/build/libs/*.jar /usr/lib/horseshoe.jar

FROM jre-base AS deploy-local-jar
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /usr/lib/horseshoe.jar

FROM deploy-${DEPLOY} AS deploy
USER horseshoe
RUN ["/usr/bin/java", "-jar", "/usr/lib/horseshoe.jar", "--version"]

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/lib/horseshoe.jar"]
CMD ["--version"]

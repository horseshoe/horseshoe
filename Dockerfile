FROM ubuntu AS build
COPY . /usr/src/horseshoe
WORKDIR /usr/src/horseshoe
RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y default-jdk git && \
	rm -rf /var/lib/apt/lists/*
RUN ./gradlew jar && \
	rm build/libs/*-javadoc.jar build/libs/*-sources.jar

FROM openjdk:jre-alpine AS deploy
COPY --from=build /usr/src/horseshoe/build/libs/horseshoe-*.jar /usr/bin/
RUN cd /usr/bin && ln -s horseshoe-*.jar horseshoe.jar
ENTRYPOINT ["java", "-jar", "/usr/bin/horseshoe.jar"]
CMD ["--version"]

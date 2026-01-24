FROM azul/zulu-openjdk-alpine:21-jre as builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM azul/zulu-openjdk-alpine:21-jre
RUN mkdir /app
WORKDIR /app
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder spring-dependencies/ ./
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder application/ ./
CMD exec java ${JAVA_OPTS} -Dfile.encoding=UTF-8 org.springframework.boot.loader.launch.JarLauncher
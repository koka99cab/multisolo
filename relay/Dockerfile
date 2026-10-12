FROM eclipse-temurin:25-jdk AS build
WORKDIR /source
COPY . .
RUN chmod +x gradlew && ./gradlew :relay:installDist --no-daemon --console=plain

FROM eclipse-temurin:25-jre
RUN groupadd --gid 10001 multisolo && useradd --uid 10001 --gid multisolo --create-home multisolo \
    && mkdir /data && chown multisolo:multisolo /data
COPY --from=build /source/relay/build/install/relay /app
ENV MULTISOLO_BIND=0.0.0.0 MULTISOLO_PORT=8787 MULTISOLO_DATA=/data/messages
USER 10001:10001
VOLUME /data
EXPOSE 8787
ENTRYPOINT ["/app/bin/relay"]

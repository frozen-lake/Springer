# syntax=docker/dockerfile:1
ARG BOT_IMAGE

FROM ${BOT_IMAGE} AS build
USER root
WORKDIR /build

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY Makefile ./
COPY src/ ./src/
COPY tests/ ./tests/

RUN make -j2 tests uci \
    && ./springer_tests \
    && test -x ./uci.exe

FROM ${BOT_IMAGE} AS runtime
WORKDIR /lichess-bot

COPY --from=build --chmod=0755 /build/uci.exe /opt/springer/springer-uci
COPY --chmod=0644 config.yml /lichess-bot/config/config.yml
RUN chmod 0755 /lichess-bot/config
COPY --chmod=0644 LICENSE /opt/springer/LICENSE

ENV PYTHONUNBUFFERED=1
USER 10001:10001
STOPSIGNAL SIGINT

ENTRYPOINT ["python3", "lichess-bot.py"]
CMD ["--config", "/lichess-bot/config/config.yml", "--disable_auto_logging"]
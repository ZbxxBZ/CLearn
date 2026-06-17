FROM debian:12-slim

RUN useradd --create-home --shell /usr/sbin/nologin runner \
    && apt-get update \
    && apt-get install -y --no-install-recommends libc6 \
    && rm -rf /var/lib/apt/lists/*

USER runner
WORKDIR /work

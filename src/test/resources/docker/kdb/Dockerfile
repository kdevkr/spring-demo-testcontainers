FROM ubuntu:22.04 AS builder
WORKDIR /
RUN apt-get update -y \
    && apt-get install -y wget unzip \
    && wget -O l64.zip https://l.kx.com/d/kdb%2b/4.0/l64.zip?lnum=0 \
    && mkdir -p /q \
    && mv l64.zip /q \
    && cd /q \
    && unzip l64.zip \
    && rm l64.zip

FROM ubuntu:22.04
ENV QHOME="/opt/kx/q"
ENV QLIC="/opt/kx"

ENV PATH ${PATH}:${QHOME}/l64

RUN apt-get update -y \
    && apt-get install -y rlwrap \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /var/cache/apt/lists

COPY entrypoint.sh entrypoint.sh
COPY --from=builder /q /opt/kx/q
COPY ./q.q /opt/kx/q/q.q

WORKDIR /
EXPOSE 5000
ENTRYPOINT ["sh", "entrypoint.sh"]
CMD ["q","-p","5000"]
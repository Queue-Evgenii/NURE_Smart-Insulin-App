FROM gcc:latest AS builder
COPY ./src /src
WORKDIR /src

RUN gcc -shared -fPIC -O2 -o libhba1c.so calculate.c

FROM debian:bookworm-slim
COPY --from=builder /src/libhba1c.so /libs/libhba1c.so
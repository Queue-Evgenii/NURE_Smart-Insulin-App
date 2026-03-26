FROM gcc:latest AS builder
COPY ./src /src
WORKDIR /src

RUN gcc -shared -fPIC -o libhba1c.so calculate.c

FROM alpine:latest
COPY --from=builder /src/libhba1c.so /libs/libhba1c.so
CMD ["cp", "/libs/libhba1c.so", "/dist/libhba1c.so"]
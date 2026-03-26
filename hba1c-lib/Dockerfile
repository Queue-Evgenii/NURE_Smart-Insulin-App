FROM gcc:latest AS builder

COPY ./include /app/include
COPY ./src /app/src

WORKDIR /app

RUN gcc -shared -fPIC -O2 -Iinclude -o libdiabetes.so src/*.c -lm

FROM debian:bookworm-slim

# Store the .so outside /dist so the bind mount does not mask it at build time.
# At container startup, CMD copies it into the bind-mounted /dist on the host.
COPY --from=builder /app/libdiabetes.so /libdiabetes.so

CMD ["sh", "-c", "mkdir -p /dist && cp /libdiabetes.so /dist/libdiabetes.so"]
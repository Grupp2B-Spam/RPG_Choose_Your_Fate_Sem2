# Service-owned local development storage, built from the official source release.
FROM golang:1.26.11-bookworm AS builder
ADD https://github.com/minio/minio/archive/refs/tags/RELEASE.2025-10-15T17-29-55Z.tar.gz /tmp/minio.tar.gz
RUN mkdir /src && tar -xzf /tmp/minio.tar.gz --strip-components=1 -C /src
WORKDIR /src
RUN CGO_ENABLED=0 go build -trimpath -o /out/minio .
FROM alpine:3.23
RUN apk add --no-cache ca-certificates && addgroup -S minio && adduser -S minio -G minio \
    && mkdir /data && chown minio:minio /data
COPY --from=builder /out/minio /usr/local/bin/minio
COPY --from=builder /src/LICENSE /usr/share/licenses/minio/LICENSE
LABEL org.opencontainers.image.source="https://github.com/minio/minio"
USER minio
ENTRYPOINT ["minio"]

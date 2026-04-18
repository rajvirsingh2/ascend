FROM golang:1.25-alpine AS builder

WORKDIR /app

RUN apk add --no-cache git

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux \
    go build -ldflags="-s -w" -o /bin/ascend ./cmd/server/main.go

RUN CGO_ENABLED=0 GOOS=linux go build \
    -ldflags="-s -w" -o /bin/ascend-worker ./cmd/worker/main.go

FROM gcr.io/distroless/static-debian12
COPY --from=builder /bin/ascend /ascend
COPY --from=builder /bin/ascend-worker /ascend-worker
EXPOSE 8080
USER nonroot:nonroot

CMD ["/ascend"]
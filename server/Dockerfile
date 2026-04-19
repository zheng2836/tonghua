FROM golang:1.22 AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /out/tonghua-api ./cmd/api

FROM debian:bookworm-slim
WORKDIR /app
COPY --from=build /out/tonghua-api /app/tonghua-api
EXPOSE 8080
CMD ["/app/tonghua-api"]

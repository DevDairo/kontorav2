# syntax=docker/dockerfile:1

FROM node:22.23.1-alpine3.24

WORKDIR /app

ENV NODE_ENV=production

RUN apk add --no-cache tar

COPY package.json ./
COPY server ./server

RUN mkdir -p /var/lib/kontora-ops/backups /source/storage \
  && chmod 0700 /var/lib/kontora-ops/backups

EXPOSE 8091

HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=5s \
  CMD wget -q -O /dev/null http://127.0.0.1:8091/healthz || exit 1

CMD ["node", "server/server.mjs"]


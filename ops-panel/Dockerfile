# syntax=docker/dockerfile:1

FROM node:22.23.1-alpine3.24

WORKDIR /app

ENV NODE_ENV=production

COPY --chown=node:node package.json pnpm-lock.yaml ./
RUN corepack enable \
  && corepack prepare pnpm@11.9.0 --activate \
  && pnpm install --prod --frozen-lockfile \
  && pnpm store prune

COPY --chown=node:node public ./public
COPY --chown=node:node server ./server

RUN mkdir -p /var/lib/kontora-ops \
  && chown node:node /var/lib/kontora-ops

USER node

EXPOSE 8090

HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=5s \
  CMD wget -q -O /dev/null http://127.0.0.1:8090/healthz || exit 1

CMD ["node", "server/server.mjs"]

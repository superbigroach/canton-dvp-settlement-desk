# CrossDesk reference signer service — docs/SIGNER_PROTOCOL.md §4.
# Build:  docker build -t crossdesk/signer .
# Run:    docker run -v ./signer.yml:/app/signer.yml -e CROSSDESK_API_KEY=ck_... crossdesk/signer
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts
COPY tsconfig.json ./
COPY src ./src
RUN npm run build

FROM node:20-alpine
ENV NODE_ENV=production
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force
COPY --from=build /app/dist ./dist
COPY examples ./examples
# The state file lives here; mount a volume to keep it across restarts.
RUN mkdir -p /app/data && chown -R node:node /app
USER node
ENV SIGNER_CONFIG=/app/signer.yml
ENV SIGNER_STATE=/app/data/signer-state.json
ENV PORT=8787
EXPOSE 8787
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://127.0.0.1:${PORT}/health || exit 1
CMD ["node", "dist/index.js"]

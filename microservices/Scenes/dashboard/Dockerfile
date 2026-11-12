FROM node:22-alpine
WORKDIR /app
COPY package.json server.mjs ./
COPY public ./public
ENV PORT=3000 DASHBOARD_HOST=0.0.0.0
EXPOSE 3000
CMD ["node", "server.mjs"]

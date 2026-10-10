# Stage 1: Build the React application
FROM node:24-alpine AS builder
WORKDIR /app

# Define a build argument (default value for local dev)
ARG VITE_API_URL=/messenger/api/v1
ARG VITE_WS_API_URL=/messenger/api/v1/ws

# Promote ARGs to ENV so `npm run build` can see them
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WEB_SOCKET_API_URL=$VITE_WS_API_URL

COPY package*.json ./

# Configure npm to use the npmmirror registry
# Configure npm to use the npmmirror registry
RUN npm config set registry https://registry.npmmirror.com && \
    npm config set fetch-retries 5 && \
    npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000

RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/ssl /etc/nginx/ssl
EXPOSE 443

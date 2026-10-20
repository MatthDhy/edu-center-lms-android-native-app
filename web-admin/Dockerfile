# ─────────────────────────────────────────────────────────
# Stage 1 — Build React app
# ─────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files trước để tận dụng Docker layer cache
COPY package*.json ./
RUN npm ci --silent

# Copy toàn bộ source code
COPY . .

# Khi build trong Docker: API_BASE_URL để trống → axios dùng relative URL
# Nginx sẽ proxy /api/* và /files/* đến backend:8080
ENV VITE_API_BASE_URL=""

RUN npm run build

# ─────────────────────────────────────────────────────────
# Stage 2 — Serve bằng Nginx (nhẹ, production-ready)
# ─────────────────────────────────────────────────────────
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Xoá file mặc định của Nginx
RUN rm -rf ./*

# Copy file đã build từ stage 1
COPY --from=builder /app/dist .

# Copy cấu hình Nginx custom
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app

# Cache deps
COPY package*.json ./
RUN npm ci

# Build
COPY . .
RUN npm run build

# --- Runtime stage ---
FROM nginx:1.27-alpine
# Nginx conf with proxy /api
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Static app
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

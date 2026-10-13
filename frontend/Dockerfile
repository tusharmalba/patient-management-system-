# ╔══════════════════════════════════════════════════════════════════════╗
# ║                   FRONTEND DOCKERFILE                                ║
# ║                                                                       ║
# ║  Stage 1: Node.js builds the React app (npm run build → /dist)       ║
# ║  Stage 2: Nginx serves the static files                               ║
# ╚══════════════════════════════════════════════════════════════════════╝

# Stage 1: Build React with Vite
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first (layer caching for node_modules)
COPY package*.json ./
RUN npm ci --silent       # ci = clean install, exact versions, faster than npm install

# Copy source and build
COPY . .

# VITE_API_URL is injected at build time via build arg
# Client will call this URL for all API requests
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build         # Outputs to /app/dist

# Stage 2: Serve with Nginx
FROM nginx:alpine AS runtime

# Copy custom Nginx config (handles React Router client-side routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built React files to Nginx's serving directory
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# Test Nginx config and start
CMD ["nginx", "-g", "daemon off;"]

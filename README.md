# chatly
Simple messenger app. Used tools:
 - React + Vite (frontend)
 - Spring Boot (backend)
 - PostgreSQL (persisitent storage)
 - Redis (cache)
 - Minio (object storage for media files like images, video, audio)
 - Nginx (reverse proxy)


Install self-signed certificates for Nginx and browser to use:
`openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout nginx/ssl/localhost.key \
  -out nginx/ssl/localhost.crt \
  -days 365 \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost"`
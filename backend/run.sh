#!/bin/bash
export SPRING_PROFILES_ACTIVE="dev"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/makershub_dev"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="Kindness#24"
# Using a 512+ bit base64-encoded secret key to satisfy HMAC-SHA512 key size requirements (minimum 64 bytes)
export JWT_SECRET="bXlfc3VwZXJfc2VjcmV0X2tleV9mb3JfbWFrZXJzaHViX2p3dF8yMDI0X3ZlcnlfbG9uZ19hbmRfc2VjdXJlX2tleV9leHRlbnNpb25fMTIzNDU="
export MAKERSHUB_CORS_ALLOWED_ORIGINS="http://localhost:3000"
export SERVER_PORT="8081" # Avoid local port conflicts if 8080 is occupied

mvn spring-boot:run

#!/bin/bash

# Build and push the Docker image
echo "Building and pushing Docker image..."
if docker buildx build --platform linux/amd64,linux/arm64 \
    -t kasbench/globeco-pricing-service:latest \
    -t kasbench/globeco-pricing-service:1.0.2 \
    --push .; then
    echo "Docker build successful."
else
    echo "Docker build failed. Skipping Kubernetes deployment update."
    exit 1
fi

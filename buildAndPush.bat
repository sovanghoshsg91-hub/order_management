@echo off
SET AWS_ACCOUNT_ID=859328854484
SET AWS_REGION=ap-south-1
SET ECR=%AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com

echo Login to ECR...
aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ECR%
IF %ERRORLEVEL% NEQ 0 (echo ECR login failed! & exit /b 1)
echo Login Succeeded!

echo Building api-gateway...
docker build --no-cache -f Dockerfile.api-gateway -t %ECR%/partner-orders/api-gateway:latest .
IF %ERRORLEVEL% NEQ 0 (echo api-gateway build failed! & exit /b 1)
docker push %ECR%/partner-orders/api-gateway:latest
echo api-gateway pushed!

echo Building partner-service...
docker build --no-cache -f Dockerfile.partner-service -t %ECR%/partner-orders/partner-service:latest .
IF %ERRORLEVEL% NEQ 0 (echo partner-service build failed! & exit /b 1)
docker push %ECR%/partner-orders/partner-service:latest
echo partner-service pushed!

echo Building order-service...
docker build --no-cache -f Dockerfile.order-service -t %ECR%/partner-orders/order-service:latest .
IF %ERRORLEVEL% NEQ 0 (echo order-service build failed! & exit /b 1)
docker push %ECR%/partner-orders/order-service:latest
echo order-service pushed!

echo Building fulfilment-service...
docker build --no-cache -f Dockerfile.fulfilment-service -t %ECR%/partner-orders/fulfilment-service:latest .
IF %ERRORLEVEL% NEQ 0 (echo fulfilment-service build failed! & exit /b 1)
docker push %ECR%/partner-orders/fulfilment-service:latest
echo fulfilment-service pushed!

echo All done!
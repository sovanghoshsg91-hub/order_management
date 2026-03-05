@echo off
REM ── ECS Setup Script ─────────────────────────────────────────────────────────
REM Run ONCE before deploying services
REM Creates: CloudWatch log groups, ECS cluster, registers task definitions

SET AWS_REGION=ap-south-1

echo.
echo ── Step 1: Create CloudWatch Log Groups ─────────────────────────────────────
aws logs create-log-group --log-group-name /ecs/redis              --region %AWS_REGION%
aws logs create-log-group --log-group-name /ecs/api-gateway        --region %AWS_REGION%
aws logs create-log-group --log-group-name /ecs/partner-service    --region %AWS_REGION%
aws logs create-log-group --log-group-name /ecs/order-service      --region %AWS_REGION%
aws logs create-log-group --log-group-name /ecs/fulfilment-service --region %AWS_REGION%
echo Log groups created!

echo.
echo ── Step 2: Create ECS Cluster ───────────────────────────────────────────────
aws ecs create-cluster --cluster-name partner-orders-cluster --region %AWS_REGION%
echo Cluster created!

echo.
echo ── Step 3: Register Task Definitions ────────────────────────────────────────
aws ecs register-task-definition --cli-input-json file://ecs-task-redis.json             --region %AWS_REGION%
aws ecs register-task-definition --cli-input-json file://ecs-task-api-gateway.json       --region %AWS_REGION%
aws ecs register-task-definition --cli-input-json file://ecs-task-partner-service.json   --region %AWS_REGION%
aws ecs register-task-definition --cli-input-json file://ecs-task-order-service.json     --region %AWS_REGION%
aws ecs register-task-definition --cli-input-json file://ecs-task-fulfilment-service.json --region %AWS_REGION%
echo Task definitions registered!

echo.
echo ── All setup complete! ───────────────────────────────────────────────────────
echo Next: run buildAndPush.bat to build and push images
echo Then: create ECS services in AWS Console

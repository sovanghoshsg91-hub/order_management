@echo off
REM ── Run after getting Security Group ID ──────────────────────────────────────
REM Usage: createServices_step2.bat sg-xxxxxxxxx

SET SG=%1
SET AWS_REGION=ap-south-1
SET CLUSTER=partner-orders-cluster
SET SUBNETS=subnet-0f65cd2d81dd406df,subnet-082cab2df145ea110,subnet-0013b053dc153ec82

IF "%SG%"=="" (
  echo Usage: createServices_step2.bat sg-xxxxxxxxx
  exit /b 1
)

echo Using Security Group: %SG%

echo.
echo ── Opening ports ────────────────────────────────────────────────────────────
aws ec2 authorize-security-group-ingress --group-id %SG% --protocol tcp --port 6379 --cidr 0.0.0.0/0 --region %AWS_REGION%
aws ec2 authorize-security-group-ingress --group-id %SG% --protocol tcp --port 8081 --cidr 0.0.0.0/0 --region %AWS_REGION%
aws ec2 authorize-security-group-ingress --group-id %SG% --protocol tcp --port 8082 --cidr 0.0.0.0/0 --region %AWS_REGION%
aws ec2 authorize-security-group-ingress --group-id %SG% --protocol tcp --port 8083 --cidr 0.0.0.0/0 --region %AWS_REGION%
aws ec2 authorize-security-group-ingress --group-id %SG% --protocol tcp --port 8090 --cidr 0.0.0.0/0 --region %AWS_REGION%
echo Ports opened!

echo.
echo ── Creating Redis service ────────────────────────────────────────────────────
aws ecs create-service ^
  --cluster %CLUSTER% ^
  --service-name redis ^
  --task-definition redis ^
  --desired-count 1 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[%SUBNETS%],securityGroups=[%SG%],assignPublicIp=ENABLED}" ^
  --region %AWS_REGION%
echo Redis service created!

echo.
echo ── Waiting 30s for Redis to start ───────────────────────────────────────────
timeout /t 30 /nobreak

echo.
echo ── Creating partner-service ─────────────────────────────────────────────────
aws ecs create-service ^
  --cluster %CLUSTER% ^
  --service-name partner-service ^
  --task-definition partner-service ^
  --desired-count 1 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[%SUBNETS%],securityGroups=[%SG%],assignPublicIp=ENABLED}" ^
  --region %AWS_REGION%
echo partner-service created!

echo.
echo ── Creating order-service ───────────────────────────────────────────────────
aws ecs create-service ^
  --cluster %CLUSTER% ^
  --service-name order-service ^
  --task-definition order-service ^
  --desired-count 1 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[%SUBNETS%],securityGroups=[%SG%],assignPublicIp=ENABLED}" ^
  --region %AWS_REGION%
echo order-service created!

echo.
echo ── Creating fulfilment-service ──────────────────────────────────────────────
aws ecs create-service ^
  --cluster %CLUSTER% ^
  --service-name fulfilment-service ^
  --task-definition fulfilment-service ^
  --desired-count 1 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[%SUBNETS%],securityGroups=[%SG%],assignPublicIp=ENABLED}" ^
  --region %AWS_REGION%
echo fulfilment-service created!

echo.
echo ── Creating api-gateway ─────────────────────────────────────────────────────
aws ecs create-service ^
  --cluster %CLUSTER% ^
  --service-name api-gateway ^
  --task-definition api-gateway ^
  --desired-count 1 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[%SUBNETS%],securityGroups=[%SG%],assignPublicIp=ENABLED}" ^
  --region %AWS_REGION%
echo api-gateway created!

echo.
echo ── All 5 services created! ──────────────────────────────────────────────────
echo Check ECS Console: https://ap-south-1.console.aws.amazon.com/ecs

@echo off
REM ── Create ECS Services ───────────────────────────────────────────────────────

SET AWS_REGION=ap-south-1
SET CLUSTER=partner-orders-cluster
SET VPC=vpc-0fc065101697665de
SET SUBNETS=subnet-0f65cd2d81dd406df,subnet-082cab2df145ea110,subnet-0013b053dc153ec82
SET SG=PLACEHOLDER_SG

echo.
echo ── Step 1: Create Security Group ────────────────────────────────────────────
aws ec2 create-security-group ^
  --group-name partner-orders-sg ^
  --description "Partner Orders Platform Security Group" ^
  --vpc-id %VPC% ^
  --region %AWS_REGION%

echo.
echo ── Step 2: Open required ports ──────────────────────────────────────────────
REM Get SG ID first — run manually and update SG variable above
REM Then run steps 3-7

echo.
echo Security group created!
echo IMPORTANT: Copy the GroupId from above output
echo Then run: createServices_step2.bat ^<GroupId^>

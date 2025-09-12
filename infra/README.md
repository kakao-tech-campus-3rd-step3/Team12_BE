# Uni-Schedule Infra

- 이 프로젝트에서는 인프라를 코드(IaC, Infrastructure as Code) 로 관리하기 위해 Terraform을 사용했습니다.
- Terraform을 통해 반복 가능한 인프라 구성을 자동화하고, 변경 이력을 코드로 추적할 수 있습니다.

## 현재 구성된 리소스
- IAM
    - uni-schedule-user: 관리자 권한(AdministratorAccess)을 가진 IAM User
    - ecsTaskExecutionRole: ECS Task가 ECR에서 이미지 Pull 및 CloudWatch 로그를 기록할 수 있도록 하는 IAM Role

- VPC
    - uni-schedule-vpc: 기본 VPC (CIDR: 10.0.0.0/16)
    - 2개의 퍼블릭 서브넷과 인터넷 게이트웨이(IGW), 라우트 테이블 및 연결 설정

- RDS
    - MySQL 8.0 인스턴스 (db.t4g.micro)
    - 퍼블릭 서브넷 기반 DB Subnet Group
    - 보안 그룹(database_sg)을 통한 3306 포트 접근 허용
    - 사용자명/비밀번호는 SSM Parameter Store 에서 안전하게 관리

- ECR
    - uni-schedule-backend-ecr-repo: 백엔드 도커 이미지를 저장하는 ECR 리포지토리

- Security Group
    - DB 접근용 보안 그룹 (3306 포트 오픈)
    - ALB 보안 그룹: 80 포트 외부 오픈
    - ECS Task 보안 그룹: ALB에서 들어오는 트래픽(8080) 허용

- ECS & ALB
    - ECS Cluster (uni-schedule-cluster)
    - ECS Task Definition (uni-schedule-task)
        - ARM64 / Fargate 기반
        - Backend 컨테이너 (포트 8080)
        - DB 연결 정보(Environment 변수 → SSM Parameter Store에서 주입)
        - CloudWatch Logs(/ecs/backend) 연동
    - ECS Service (uni-schedule-service)
    - Application Load Balancer
        - 리스너(80) → Target Group(8080) 포워딩
        - Health Check /actuator/health

- CloudWatch
    - Log Group: /ecs/backend (보존 기간 14일)

## 앞으로 남은 작업
- Frontend Hosting
    - React 프론트엔드를 S3 + CloudFront 기반 정적 호스팅

- 네트워크 개선
    - 현재는 RDS와 ECS Task가 퍼블릭 서브넷에 위치
    - 이후 프라이빗 서브넷으로 이전하여 보안 강화
    - 단, NAT 게이트웨이 사용 시 추가 비용 발생 → 초기 개발 단계에서는 퍼블릭 서브넷 기반으로 구성

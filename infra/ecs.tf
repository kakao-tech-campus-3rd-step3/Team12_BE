resource "aws_ecs_cluster" "this" {
  name = "uni-schedule-cluster"
}

resource "aws_ecs_task_definition" "this" {
  family             = "uni-schedule-task"
  requires_compatibilities = ["FARGATE"]
  network_mode       = "awsvpc"
  cpu                = "256"
  memory             = "512"
  execution_role_arn = aws_iam_role.ecs_task_execution.arn
  task_role_arn      = aws_iam_role.ecs_task.arn

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = "${data.aws_caller_identity.current.account_id}.dkr.ecr.ap-northeast-2.amazonaws.com/backend:${var.backend_image_tag}",
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "SPRING_DATASOURCE_URL", value = data.aws_ssm_parameter.db_url.value },
        { name = "JWT_ACCESS_TOKEN_TIMEOUT_SEC", value = data.aws_ssm_parameter.jwt_access_token_timeout_sec.value },
        { name = "JWT_REFRESH_TOKEN_TIMEOUT_SEC", value = data.aws_ssm_parameter.jwt_refresh_token_timeout_sec.value }
      ]

      secrets = [
        { name = "SPRING_DATASOURCE_USERNAME", valueFrom = data.aws_ssm_parameter.db_username.arn },
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = data.aws_ssm_parameter.db_password.arn },
        { name = "JWT_SECRET", valueFrom = data.aws_ssm_parameter.jwt_secret.arn }
      ]

      logConfiguration = {
        logDriver = "awslogs",
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend.name,
          "awslogs-region"        = data.aws_region.current.name,
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_lb" "this" {
  name               = "uni-schedule-lb"
  internal           = false
  load_balancer_type = "application"
  security_groups = [aws_security_group.lb.id]
  subnets            = aws_subnet.public_subnet[*].id
}

resource "aws_lb_target_group" "this" {
  name        = "uni-schedule-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.this.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200-399"
    interval            = 30
    timeout             = 20
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
}

resource "aws_lb_listener" "this" {
  load_balancer_arn = aws_lb.this.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}

resource "aws_ecs_service" "this" {
  name                              = "uni-schedule-service"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.this.arn
  desired_count                     = 1
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 300

  network_configuration {
    subnets          = aws_subnet.public_subnet[*].id
    security_groups = [aws_security_group.ecs_instance.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = "backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.this]
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/backend"
  retention_in_days = 14

  tags = {
    Name = "backend-log-group"
  }
}

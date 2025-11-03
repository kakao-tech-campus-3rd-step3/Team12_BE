resource "aws_ecs_cluster" "this" {
  name = "uni-schedule-cluster"
}

resource "aws_ecs_task_definition" "this" {
  family                   = "uni-schedule-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"  # 256
  memory                   = "1024" # 512
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

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
        { name = "TZ", value = "Asia/Seoul" },
        { name = "SPRING_DATASOURCE_URL", value = data.aws_ssm_parameter.db_url.value },
        { name = "JWT_ACCESS_TOKEN_TIMEOUT_SEC", value = data.aws_ssm_parameter.jwt_access_token_timeout_sec.value },
        { name = "JWT_REFRESH_TOKEN_TIMEOUT_SEC", value = data.aws_ssm_parameter.jwt_refresh_token_timeout_sec.value },
        { name = "REDIS_HOST", value = data.aws_ssm_parameter.redis_host.value },
        { name = "REDIS_USERNAME", value = data.aws_ssm_parameter.redis_username.value },
        { name = "REDIS_PORT", value = "17830" },
        { name = "GOOGLE_REDIRECT_URI", value = data.aws_ssm_parameter.google_redirect_url.value }
      ]

      secrets = [
        { name = "SPRING_DATASOURCE_USERNAME", valueFrom = data.aws_ssm_parameter.db_username.arn },
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = data.aws_ssm_parameter.db_password.arn },
        { name = "JWT_SECRET", valueFrom = data.aws_ssm_parameter.jwt_secret.arn },
        { name = "OPENAI_API_KEY", valueFrom = data.aws_ssm_parameter.openai_api_key.arn },
        { name = "REDIS_PASSWORD", valueFrom = data.aws_ssm_parameter.redis_password.arn },
        { name = "MAIL_USERNAME", valueFrom = data.aws_ssm_parameter.mail_username.arn },
        { name = "MAIL_PASSWORD", valueFrom = data.aws_ssm_parameter.mail_password.arn },
        { name = "GOOGLE_CLIENT_ID", valueFrom = data.aws_ssm_parameter.google_client_id.arn },
        { name = "GOOGLE_CLIENT_SECRET", valueFrom = data.aws_ssm_parameter.google_client_secret.arn }
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
  security_groups    = [aws_security_group.lb.id]
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
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = aws_acm_certificate.this.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }

  depends_on = [aws_acm_certificate_validation.this]
}

resource "aws_ecs_service" "this" {
  name                              = "uni-schedule-service"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.this.arn
  desired_count                     = 2
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 300

  network_configuration {
    subnets          = aws_subnet.public_subnet[*].id
    security_groups  = [aws_security_group.ecs_instance.id]
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

resource "aws_appautoscaling_target" "ecs" {
  max_capacity       = 5
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.this.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_cloudwatch_metric_alarm" "scale_out_alarm" {
  alarm_name          = "scale-out-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  statistic           = "Average"
  threshold           = 50
  alarm_description   = "Scale out when CPU > 50% for 1 minute"
  dimensions = {
    ClusterName = aws_ecs_cluster.this.name
    ServiceName = aws_ecs_service.this.name
  }
  alarm_actions = [aws_appautoscaling_policy.scale_out_step.arn]
}

resource "aws_appautoscaling_policy" "scale_out_step" {
  name               = "scale-out-step"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = "ecs"

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 30
    metric_aggregation_type = "Average"

    step_adjustment {
      metric_interval_lower_bound = 0
      scaling_adjustment          = 1
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "scale_in_alarm" {
  alarm_name          = "scale-in-alarm"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  period              = 60
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  statistic           = "Average"
  threshold           = 20
  alarm_description   = "Scale in when CPU < 20% for 1 minute"
  dimensions = {
    ClusterName = aws_ecs_cluster.this.name
    ServiceName = aws_ecs_service.this.name
  }
  alarm_actions = [aws_appautoscaling_policy.scale_in_step.arn]
}

resource "aws_appautoscaling_policy" "scale_in_step" {
  name               = "scale-in-step"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = "ecs"

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 30
    metric_aggregation_type = "Average"

    step_adjustment {
      metric_interval_upper_bound = 0
      scaling_adjustment          = -1
    }
  }
}

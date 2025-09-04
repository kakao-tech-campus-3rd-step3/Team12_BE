resource "aws_ecr_repository" "backend" {
  name                 = "backend"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "uni-schedule-backend-ecr-repo"
    Description = "ECR repository for Backend in uni-schedule"
  }
}

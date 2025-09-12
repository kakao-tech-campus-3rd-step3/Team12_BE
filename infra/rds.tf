resource "aws_db_subnet_group" "db_subnet_group" {
  name       = "uni-schedule-db-subnet-group"
  subnet_ids = aws_subnet.public_subnet[*].id

  tags = {
    Name = "uni-schedule-db-subnet-group"
  }
}

resource "aws_db_instance" "db" {
  identifier            = "uni-schedule-db"
  engine                = "mysql"
  engine_version        = "8.0.41"
  instance_class        = "db.t4g.micro"
  allocated_storage     = 20
  max_allocated_storage = 1000
  storage_encrypted     = true
  db_name               = "uni_schedule_db"
  username              = data.aws_ssm_parameter.db_username.value
  password              = data.aws_ssm_parameter.db_password.value
  db_subnet_group_name  = aws_db_subnet_group.db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.database_sg.id]
  skip_final_snapshot   = true
  multi_az              = false
  publicly_accessible   = true

  tags = {
    Name        = "uni-schedule-db"
    Project     = "uni-schedule"
    Environment = "dev"
  }
}

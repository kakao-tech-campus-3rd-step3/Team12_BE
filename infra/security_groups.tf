resource "aws_security_group" "database_sg" {
  name        = "uni-schedule database security group"
  description = "Enable MySQL access on port 3306"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "MySQL access from anywhere"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "uni-schedule database security group"
  }
}

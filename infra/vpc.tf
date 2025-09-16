resource "aws_vpc" "this" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name    = "uni-schedule-vpc"
    Project = "uni-schedule"
  }
}

data "aws_availability_zones" "available_zones" {}

resource "aws_subnet" "public_subnet" {
  count                   = 2
  vpc_id                  = aws_vpc.this.id
  cidr_block = cidrsubnet(aws_vpc.this.cidr_block, 8, count.index + 10)
  map_public_ip_on_launch = true
  availability_zone       = data.aws_availability_zones.available_zones.names[count.index]

  tags = {
    Name    = "uni-schedule-public-subnet-${count.index + 1}"
    Project = "uni-schedule"
  }
}

# resource "aws_subnet" "private_subnet" {
#   count                   = 2
#   vpc_id                  = aws_vpc.this.id
#   cidr_block = cidrsubnet(aws_vpc.this.cidr_block, 8, count.index + 1)
#   map_public_ip_on_launch = false
#   availability_zone       = data.aws_availability_zones.available_zones.names[count.index]
#
#   tags = {
#     Name    = "uni-schedule-private-subnet-${count.index + 1}"
#     Project = "uni-schedule"
#   }
# }

resource "aws_internet_gateway" "internet_gateway" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name    = "uni-schedule-igw"
    Project = "uni-schedule"
  }
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.internet_gateway.id
  }

  tags = {
    Name    = "uni-schedule-public-rt"
    Project = "uni-schedule"
  }
}

resource "aws_route_table_association" "public_association" {
  count = length(aws_subnet.public_subnet)
  subnet_id      = aws_subnet.public_subnet[count.index].id
  route_table_id = aws_route_table.public_route_table.id
}

# resource "aws_vpc_endpoint" "ecr_api" {
#   vpc_id              = aws_vpc.this.id
#   service_name        = "com.amazonaws.ap-northeast-2.ecr.api"
#   vpc_endpoint_type   = "Interface"
#   subnet_ids          = aws_subnet.private_subnet[*].id
#   security_group_ids = [aws_security_group.ecs_instance.id]
#   private_dns_enabled = true
#   tags = {
#     Name    = "uni-schedule-ecr-api-endpoint"
#     Project = "uni-schedule"
#   }
# }
#
# resource "aws_vpc_endpoint" "ecr_dkr" {
#   vpc_id              = aws_vpc.this.id
#   service_name        = "com.amazonaws.ap-northeast-2.ecr.dkr"
#   vpc_endpoint_type   = "Interface"
#   subnet_ids          = aws_subnet.private_subnet[*].id
#   security_group_ids = [aws_security_group.ecs_instance.id]
#   private_dns_enabled = true
#   tags = {
#     Name    = "uni-schedule-ecr-dkr-endpoint"
#     Project = "uni-schedule"
#   }
# }
#
# resource "aws_vpc_endpoint" "ssm" {
#   vpc_id              = aws_vpc.this.id
#   service_name        = "com.amazonaws.ap-northeast-2.ssm"
#   vpc_endpoint_type   = "Interface"
#   subnet_ids          = aws_subnet.private_subnet[*].id
#   security_group_ids = [aws_security_group.ecs_instance.id]
#   private_dns_enabled = true
#   tags = {
#     Name    = "uni-schedule-ssm-endpoint"
#     Project = "uni-schedule"
#   }
# }
#
# resource "aws_vpc_endpoint" "ec2messages" {
#   vpc_id              = aws_vpc.this.id
#   service_name        = "com.amazonaws.ap-northeast-2.ec2messages"
#   vpc_endpoint_type   = "Interface"
#   subnet_ids          = aws_subnet.private_subnet[*].id
#   security_group_ids = [aws_security_group.ecs_instance.id]
#   private_dns_enabled = true
#   tags = {
#     Name    = "uni-schedule-ec2messages-endpoint"
#     Project = "uni-schedule"
#   }
# }

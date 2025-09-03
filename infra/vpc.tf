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
  cidr_block              = cidrsubnet(aws_vpc.this.cidr_block, 8, count.index)
  map_public_ip_on_launch = true
  availability_zone       = data.aws_availability_zones.available_zones.names[count.index]

  tags = {
    Name    = "uni-schedule-public-subnet-${count.index + 1}"
    Project = "uni-schedule"
  }
}

resource "aws_internet_gateway" "internet_gateway" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name    = "uni-schedule-igw"
    Project = "uni-schedule"
  }
}

resource "aws_route_table" "route_table" {
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

resource "aws_route_table_association" "this" {
  count          = length(aws_subnet.public_subnet)
  subnet_id      = aws_subnet.public_subnet[count.index].id
  route_table_id = aws_route_table.route_table.id
}

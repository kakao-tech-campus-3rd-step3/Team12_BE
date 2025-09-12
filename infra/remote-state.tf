data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

resource "aws_s3_bucket" "remote_state" {
  bucket = "uni-schedule-remote-state-bucket"

  tags = {
    Name    = "uni-schedule-remote-state-bucket"
    Project = "uni-schedule"
  }
}
